package com.hanghae99.finalproject.chatRoom.service;

import com.hanghae99.finalproject.chatRoom.dto.ChatRoomDto;
import com.hanghae99.finalproject.chatRoom.dto.ChatUserDto;
import com.hanghae99.finalproject.chatRoom.dto.LastMessageDto;
import com.hanghae99.finalproject.chatRoom.dto.RoomDto;
import com.hanghae99.finalproject.chatRoom.model.Message;
import com.hanghae99.finalproject.chatRoom.model.Room;
import com.hanghae99.finalproject.chatRoom.model.UserRoom;
import com.hanghae99.finalproject.chatRoom.repository.MessageRepository;
import com.hanghae99.finalproject.chatRoom.repository.RoomRepository;
import com.hanghae99.finalproject.chatRoom.repository.UserRoomRepository;
import com.hanghae99.finalproject.common.exception.CustomException;
import com.hanghae99.finalproject.common.exception.ErrorCode;
import com.hanghae99.finalproject.mail.dto.MailDto;
import com.hanghae99.finalproject.mail.service.MailService;
import com.hanghae99.finalproject.post.model.Post;
import com.hanghae99.finalproject.post.repository.PostRepository;
import com.hanghae99.finalproject.security.UserDetailsImpl;
import com.hanghae99.finalproject.sse.model.NotificationType;
import com.hanghae99.finalproject.sse.service.NotificationService;
import com.hanghae99.finalproject.common.timeConversion.MessageTimeConversion;
import com.hanghae99.finalproject.user.model.User;
import com.hanghae99.finalproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;
    private final MessageRepository messageRepository;
    private final MailService mailService;
    private final NotificationService notificationService;


    @Transactional
    public RoomDto.Response createRoomService(RoomDto.Request roomDto, UserDetailsImpl userDetails) throws MessagingException {

        Post post = postRepository.findById(roomDto.getPostId()).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND)
        );

        User user = userDetails.getUser(); //???????????? ???????????? ??????

        User toUser = userRepository.findById(roomDto.getToUserId()).orElseThrow( //????????? ??????
                () ->  new CustomException(ErrorCode.NOT_FOUND_USER_INFO)
        );

        // ?????? '?????????'??? ??? ?????? ????????????.
        List<Room> checkRoomList = roomRepository.findByRoomPostId(post.getId());

        //????????? / ???pk / roomPostId
        for (Room room : checkRoomList) {
            UserRoom checkUserRoom = userRoomRepository.findByRoomAndUserAndToUser(room, user, toUser);
            //?????? ???????????? ?????????, ???????????? ???????????? ????????? ????????? ????????? ????????? ??????
            if (checkUserRoom != null) { //?????? ???????????? ?????? ?????? ????????????.
                throw new CustomException(ErrorCode.ALREADY_EXISTS_CHAT_ROOM);
            }
        }

        // ???????????? ?????? ?????? ????????? ???????????? ????????? ????????? ??????
        String roomName = UUID.randomUUID().toString();

        //?????????
        Room room = Room.builder()
                .roomName(roomName) //?????????
                .roomPostId(post.getId()) // ?????? ????????? ????????? ID
                .build();
        roomRepository.save(room); //?????? ??????

        // userRoom two create

        //????????? ?????? ???????????? ????????? ????????? ?????? toUser
        UserRoom userRoom = UserRoom.builder()
                .room(room)
                .user(user)
                .toUser(toUser)
                .lastMessageId(null)
                .build();
        userRoomRepository.save(userRoom);

        //???????????? ????????? ?????? ???????????? ????????? ????????? ??????
        UserRoom toUserRoom = UserRoom.builder()
                .room(room)
                .user(toUser)
                .toUser(user)
                .lastMessageId(null)
                .build();
        userRoomRepository.save(toUserRoom);

        // ????????? ?????? ????????? ?????? Dto
        ChatUserDto chatUserDto = ChatUserDto.builder()
                .userId(toUser.getId())
                .profileImg(toUser.getProfileImg())
                .nickname(toUser.getNickname())
                .build();


        RoomDto.Response response = RoomDto.Response.builder()
                .roomName(room.getRoomName()) //?????? ???
                .user(chatUserDto) //????????? ??????
                .build();

        // ????????? ?????? ????????? ??????????????? ?????? ?????? ??????(???????????? ????????? ???????????? ?????????)
        if (toUser.getIsVerifiedEmail() != null) {
            mailService.chatOnEmailBuilder(MailDto.builder()
                    .toUserId(toUser.getId())
                    .toEmail(toUser.getEmail())
                    .toNickname(toUser.getNickname())
                    .fromNickname(post.getUser().getNickname())
                    .fromProfileImg(post.getUser().getProfileImg())
                    .postId(post.getId())
                    .postTitle(post.getTitle())
                    .build());
        }

        //?????? ????????? ???????????? url
        String Url = "https://www.everymohum.com/chatlist";
        //?????? ?????? ??? ????????? ?????? ???????????? ????????? ?????? ?????? ,
        String content = toUser.getNickname()+"???! ???????????? ?????? ????????? ???????????????!";
        notificationService.send(toUser,NotificationType.CHAT,content,Url);


        return response;
    }

    /*
    roomName;
    postId;
    ChatUserDto user;
    LastMessageDto lastMessage;
    CurrentState currentState;
    notReadingMessageCount;
    */

    @Transactional
    public List<ChatRoomDto> showRoomListService(UserDetailsImpl userDetails) {
        // ?????? ???????????? ????????? ????????? ?????? ???????????? ????????????.
        List<UserRoom> userRooms = userRoomRepository.findByUser(userDetails.getUser());
        List<ChatRoomDto> chatRoomDtos = new ArrayList<>();
        // ????????? ?????? ??????
        for (UserRoom userRoom : userRooms) {
            LastMessageDto lastMessageDto; //????????? ????????? ??? ??????

            //toUser ??? ??????
            ChatUserDto chatUserDto = ChatUserDto.builder()
                    .userId(userRoom.getToUser().getId()) // ????????? ?????? pk
                    .profileImg(userRoom.getToUser().getProfileImg()) //????????? ????????? ????????? ?????????
                    .nickname(userRoom.getToUser().getNickname()) //????????? ????????? ?????????
                    .build();

            // ????????????????????? ????????? ????????? ????????? ???
            if (userRoom.getLastMessageId() == null) { //????????? ???????????? ?????????
                lastMessageDto = LastMessageDto.builder()
                        .content("?????? ?????? ???????????????.")  //?????? ?????? ???????????? ???????????? ??????
                        .createdAt(MessageTimeConversion.timeConversion(userRoom.getCreatedAt()))
                        .build();
            } else {
                // ????????? ???????????? ??????????????? , == ????????? ????????? ????????????
                Message message = messageRepository.getById(userRoom.getLastMessageId());
                // ?????? ???????????? ?????? ?????? ??? ????????? ???????????? ?????? ???????????? ???????????? 1??? ?????????.
//                if(!Objects.equals(message.getUser().getId(), userDetails.getUser().getId())){
//                    userRoom.countChange();
//                }

                // ????????? ????????? DTO ??? ????????? ????????? ????????????.
                lastMessageDto = LastMessageDto.builder()
                        .content(message.getContent())
                        .createdAt(MessageTimeConversion.timeConversion(message.getCreatedAt()))
                        .build();
            }
            //?????? userRoom pk??? ???????????? ????????? ????????? ????????????.
            Post post = postRepository.findById(userRoom.getRoom().getRoomPostId()).orElse(null);

            ChatRoomDto chatRoomDto;
            //?????? ?????????, ????????? pk , ???????????? , ?????????????????? , ????????? ,
            //???????????? ???????????? , ???????????? ????????????.
            chatRoomDto = ChatRoomDto.builder()
                    .roomName(userRoom.getRoom().getRoomName())
                    .postId(userRoom.getRoom().getRoomPostId())
                    .user(chatUserDto)
                    .lastMessage(lastMessageDto)
                    .lastMessageTime(lastMessageDto.getCreatedAt())
                    .build();
            chatRoomDtos.add(chatRoomDto);
        }
        return chatRoomDtos.stream().sorted(Comparator.comparing(ChatRoomDto::getLastMessageTime).reversed())
                .collect(Collectors.toList());
    }
}