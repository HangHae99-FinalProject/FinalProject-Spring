package com.hanghae99.finalproject.chatRoom.dto;

import com.hanghae99.finalproject.post.model.CurrentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ChatRoomDto {
    private String roomName;
    private Long postId;
    private ChatUserDto user;
    private LastMessageDto lastMessage;
    private CurrentStatus currentStatus;
    private String lastMessageTime;
    private int notReadingMessageCount;
}
