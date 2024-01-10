package com.clover.youngchat.domain.friend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.clover.youngchat.domain.friend.dto.response.FriendGetListRes;
import com.clover.youngchat.domain.friend.entity.Friend;
import com.clover.youngchat.domain.friend.repository.FriendRepository;
import com.clover.youngchat.domain.user.entity.User;
import com.clover.youngchat.domain.user.repository.UserRepository;
import com.clover.youngchat.global.exception.GlobalException;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import test.FriendTest;

@ExtendWith(MockitoExtension.class)
public class FriendServiceTest implements FriendTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendRepository friendRepository;

    @InjectMocks
    private FriendService friendService;

    @Test
    @DisplayName("친구목록에서 두명의 친구를 조회한다.")
    void getFriendList() {
        // given
        given(friendRepository.findByUser(any(User.class))).willReturn(
            Optional.of(List.of(TEST_FRIEND, TEST_ANOTHER_FRIEND)));

        // when
        FriendGetListRes actual = friendService.getFriendList(TEST_USER);

        // then
        Assertions.assertThat(actual.getUsernameList()).hasSize(2);
        verify(friendRepository, times(1)).findByUser(any(User.class));
    }

    @Test
    @DisplayName("친구목록에서 친구를 조회하는데 결과가 없다.")
    void getNotFoundFriendList() {
        // given
        given(friendRepository.findByUser(any())).willReturn(
            Optional.empty());

        // when, then
        Assertions.assertThatThrownBy(() -> friendService.getFriendList(any(User.class)))
            .isInstanceOf(GlobalException.class);

        verify(friendRepository, times(1)).findByUser(any());
    }


    @DisplayName("친구 추가 테스트")
    @Test
    void addFriendTest() {
        // given
        Long friendId = 2L;

        given(userRepository.findById(anyLong())).willReturn(Optional.of(TEST_USER));

        // when
        friendService.addFriend(friendId, TEST_USER);

        // then
        verify(userRepository, times(1)).findById(anyLong());
        verify(friendRepository, times(1)).save(any(Friend.class));
    }

    @DisplayName("본인 스스로를 친구 추가할 수 없다.")
    @Test
    void addMeFriendTest() {
        // given
        Long friendId = 1L;

        // when, then
        Assertions.assertThatThrownBy(() -> friendService.addFriend(friendId, TEST_USER))
            .isInstanceOf(GlobalException.class);
    }
}