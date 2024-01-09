package test;

import com.clover.youngchat.domain.user.entity.User;

public interface UserTest {

    Long TEST_USER_ID = 1L;
    Long TEST_ANOTHER_USER_ID = 2L;

    String TEST_USER_EMAIL = "username@email.com";
    String TEST_USER_NAME = "username";
    String TEST_USER_PASSWORD = "12345aA!";

    String TEST_ANOTHER_USER_EMAIL = "another@email.com";
    String TEST_ANOTHER_USER_NAME = "another1";
    String TEST_ANOTHER_USER_PASSWORD = "12345aA!";

    User TEST_USER = User.builder()
        .id(TEST_USER_ID)
        .email(TEST_USER_EMAIL)
        .username(TEST_USER_NAME)
        .password(TEST_USER_PASSWORD)
        .build();
    User TEST_ANOTHER_USER = User.builder()
        .id(TEST_ANOTHER_USER_ID)
        .email(TEST_ANOTHER_USER_EMAIL)
        .username(TEST_ANOTHER_USER_NAME)
        .password(TEST_ANOTHER_USER_PASSWORD)
        .build();
}
