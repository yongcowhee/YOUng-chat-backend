package com.clover.youngchat.domain.chat.repository;

import com.clover.youngchat.domain.chat.dto.response.ChatRes;
import com.clover.youngchat.domain.chat.entity.QChat;
import com.clover.youngchat.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class ChatRepositoryImpl implements ChatRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<ChatRes> findChatsByChatRoomId(Long chatRoomId, Long lastChatId, int limitSize) {
        List<ChatRes> chats = fetchChatsCursorPagination(chatRoomId, lastChatId, limitSize);
        return createSlice(chats, limitSize);
    }

    private List<ChatRes> fetchChatsCursorPagination(Long chatRoomId, Long lastChatId,
        int limitSize) {
        QChat chat = QChat.chat;
        QUser user = QUser.user;
        BooleanExpression queryCondition = createQueryCondition(chat, chatRoomId, lastChatId);

        return queryFactory
            .select(Projections.constructor(
                ChatRes.class,
                chat.id,
                user.id,
                user.username,
                user.profileImage,
                chat.message,
                chat.isDeleted,
                chat.createdAt))
            .from(chat)
            .leftJoin(chat.sender, user)
            .where(queryCondition)
            .orderBy(chat.id.desc())
            .limit(limitSize + 1) // 다음페이지가 있는지 확인하기 위해 +1 조회
            .fetch();
    }

    private BooleanExpression createQueryCondition(QChat chat, Long chatRoomId, Long lastChatId) {
        BooleanExpression condition = chat.chatRoom.id.eq(chatRoomId);
        if (lastChatId != null) {
            condition = condition.and(chat.id.lt(lastChatId));
        }
        return condition;
    }

    private Slice<ChatRes> createSlice(List<ChatRes> chats, int limitSize) {
        boolean hasNext = chats.size() > limitSize;
        if (hasNext) {
            chats.remove(limitSize); // 하나더 가져온 값 제거
        }
        return new SliceImpl<>(chats, PageRequest.of(0, limitSize), hasNext);
    }
}