package com.clover.youngchat.domain.user.service;

import static com.clover.youngchat.domain.user.constant.UserConstant.EMAIL_AUTHENTICATION;
import static com.clover.youngchat.global.exception.ResultCode.ACCESS_DENY;
import static com.clover.youngchat.global.exception.ResultCode.DUPLICATED_EMAIL;
import static com.clover.youngchat.global.exception.ResultCode.INVALID_PROFILE_IMAGE_TYPE;
import static com.clover.youngchat.global.exception.ResultCode.MISMATCH_CONFIRM_PASSWORD;
import static com.clover.youngchat.global.exception.ResultCode.MISMATCH_PASSWORD;
import static com.clover.youngchat.global.exception.ResultCode.NOT_FOUND_EMAIL;
import static com.clover.youngchat.global.exception.ResultCode.NOT_FOUND_USER;
import static com.clover.youngchat.global.exception.ResultCode.SAME_OLD_PASSWORD;
import static com.clover.youngchat.global.exception.ResultCode.UNAUTHORIZED_EMAIL;

import com.clover.youngchat.domain.auth.entity.EmailAuth;
import com.clover.youngchat.domain.user.dto.request.UserEmailAuthCheckReq;
import com.clover.youngchat.domain.user.dto.request.UserEmailAuthReq;
import com.clover.youngchat.domain.user.dto.request.UserProfileEditReq;
import com.clover.youngchat.domain.user.dto.request.UserSignupReq;
import com.clover.youngchat.domain.user.dto.request.UserUpdatePasswordReq;
import com.clover.youngchat.domain.user.dto.response.UserEmailAuthCheckRes;
import com.clover.youngchat.domain.user.dto.response.UserEmailAuthRes;
import com.clover.youngchat.domain.user.dto.response.UserEmailCheckRes;
import com.clover.youngchat.domain.user.dto.response.UserProfileEditRes;
import com.clover.youngchat.domain.user.dto.response.UserProfileGetRes;
import com.clover.youngchat.domain.user.dto.response.UserProfileSearchRes;
import com.clover.youngchat.domain.user.dto.response.UserSignupRes;
import com.clover.youngchat.domain.user.dto.response.UserUpdatePasswordRes;
import com.clover.youngchat.domain.user.entity.User;
import com.clover.youngchat.domain.user.repository.UserRepository;
import com.clover.youngchat.global.email.EmailUtil;
import com.clover.youngchat.global.exception.GlobalException;
import com.clover.youngchat.global.s3.S3Util;
import com.clover.youngchat.global.s3.S3Util.FilePath;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Util s3Util;
    private final EmailUtil emailUtil;

    @Value("${default.image.url}")
    private String defaultProfileImageUrl;


    public UserProfileGetRes getProfile(Long userId, User user) {
        String email = null;

        if (userId != null) {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
        } else {
            email = user.getEmail();
        }

        return UserProfileGetRes.to(user, email);
    }

    public UserProfileSearchRes searchProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_EMAIL));

        return UserProfileSearchRes.to(user);
    }

    public UserSignupRes signup(UserSignupReq userSignupReq) {

        validateSignup(userSignupReq);
        String encryptionPassword = passwordEncoder.encode(userSignupReq.getPassword());

        User user = User.builder()
            .email(userSignupReq.getEmail())
            .username(userSignupReq.getUsername())
            .password(encryptionPassword)
            .profileImage(defaultProfileImageUrl)
            .build();

        userRepository.save(user);

        return new UserSignupRes();
    }


    @Transactional
    public UserProfileEditRes editProfile(Long userId, UserProfileEditReq req,
        MultipartFile multipartFile,
        Long authUserId) {
        // 수정하고자 하는 프로필이 본인의 프로필이 아닌 경우
        if (!userId.equals(authUserId)) {
            throw new GlobalException(ACCESS_DENY);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_USER));

        String profileImageUrl = user.getProfileImage();

        // user가 파일을 올리지 않았을 경우 프로필 변경하지 않는 것으로 간주해서 기존 프로필 그대로 유지
        if (multipartFile == null || multipartFile.isEmpty()) {
            profileImageUrl = user.getProfileImage();
        } else {
            // user가 업로드 할 파일의 확장자가 png 인지 확인, png가 아니라면 exception 발생
            if (!Objects.equals(multipartFile.getContentType(), "image/png") &&
                !Objects.equals(multipartFile.getContentType(), "image/jpeg")) {
                throw new GlobalException(INVALID_PROFILE_IMAGE_TYPE);
            }
            // user의 프로필 url이 기본 프로필과 같지 않을 경우 s3에서 삭제
            if (!profileImageUrl.equals(defaultProfileImageUrl)) {
                s3Util.deleteFile(profileImageUrl, FilePath.PROFILE);
            }
            profileImageUrl = s3Util.uploadFile(multipartFile, FilePath.PROFILE);
        }

        user.updateProfile(req, profileImageUrl);

        return new UserProfileEditRes();
    }

    @Transactional
    public UserUpdatePasswordRes updatePassword(Long userId, UserUpdatePasswordReq req) {
        User foundUser = findUserById(userId);

        validatePasswords(req, foundUser);

        String encryptedPassword = passwordEncoder.encode(req.getNewPassword());
        foundUser.updatePassword(encryptedPassword);

        return new UserUpdatePasswordRes();
    }

    public UserEmailAuthRes sendAuthEmail(final UserEmailAuthReq req) {
        emailUtil.sendMessage(req.getEmail(), EMAIL_AUTHENTICATION);
        return new UserEmailAuthRes();
    }

    public UserEmailAuthCheckRes checkAuthEmail(final UserEmailAuthCheckReq req) {
        emailUtil.checkCode(req.getEmail(), req.getCode());
        return new UserEmailAuthCheckRes();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(NOT_FOUND_USER));
    }

    private void validateSignup(UserSignupReq req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new GlobalException(DUPLICATED_EMAIL);
        }

        EmailAuth emailAuth = emailUtil.findEmailAuth(req.getEmail());
        if (!emailAuth.isAuthenticated()) {
            throw new GlobalException(UNAUTHORIZED_EMAIL);
        }
    }

    private void validatePasswords(UserUpdatePasswordReq req, User foundUser) {
        if (!req.getNewPassword().equals(req.getCheckNewPassword())) {
            throw new GlobalException(MISMATCH_CONFIRM_PASSWORD);
        }

        if (!passwordEncoder.matches(req.getPrePassword(), foundUser.getPassword())) {
            throw new GlobalException(MISMATCH_PASSWORD);
        }

        if (req.getPrePassword().equals(req.getNewPassword())) {
            throw new GlobalException(SAME_OLD_PASSWORD);
        }
    }

    public UserEmailCheckRes checkEmailDuplicated(final String email) {
        return UserEmailCheckRes.to(userRepository.existsByEmail(email));
    }
}
