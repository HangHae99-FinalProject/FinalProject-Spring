package com.hanghae99.finalproject.user.service;

import com.hanghae99.finalproject.post.model.CurrentStatus;
import com.hanghae99.finalproject.user.dto.MajorDto;
import com.hanghae99.finalproject.exception.ErrorCode;
import com.hanghae99.finalproject.exception.PrivateException;
import com.hanghae99.finalproject.user.dto.UserApplyRequestDto;
import com.hanghae99.finalproject.user.model.Major;
import com.hanghae99.finalproject.post.model.Post;
import com.hanghae99.finalproject.user.model.User;
import com.hanghae99.finalproject.user.model.UserApply;
import com.hanghae99.finalproject.user.repository.MajorRepository;
import com.hanghae99.finalproject.post.repository.PostRepository;
import com.hanghae99.finalproject.security.UserDetailsImpl;
import com.hanghae99.finalproject.user.repository.UserApplyRepository;
import com.hanghae99.finalproject.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserApplyService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserApplyRepository userApplyRepository;

    // 모집 지원
    @Transactional
    public void apply(Long postId, UserApplyRequestDto userApplyRequestDto, UserDetailsImpl userDetails) {

        // [예외 처리] 조회하는 게시물이 존재하지 않을 경우
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PrivateException(ErrorCode.POST_NOT_FOUND)
        );

        // [예외 처리] 요청하는 유저 정보가 존재하지 않을 경우
        User user = userRepository.findByNickname(userDetails.getUser().getNickname()).orElseThrow(
                () -> new PrivateException(ErrorCode.NOT_FOUND_USER_INFO)
        );

        // [예외 처리] 본인이 모집하는 프로젝트에 신청할 경우
        if (post.getUser().getId().equals(userDetails.getUser().getId())) {
            throw new PrivateException(ErrorCode.APPLY_WRONG_ERROR);
        }

        // [예외 처리] 정원 마감, 모집 완료인 프로젝트에 신청할 경우
        if (!post.getCurrentStatus().equals(CurrentStatus.ONGOING)) {
            throw new PrivateException(ErrorCode.ALREADY_STARTED_ERROR);
        }

        // [예외 처리] 신청했던 프로젝트에 다시 신청할 경우
        if (userApplyRepository.existsByPostIdAndUserId(post.getId(), user.getId())) {
            throw new PrivateException(ErrorCode.ALREADY_APPLY_POST_ERROR);
        }

        // [유효성 검사] 선택한 지원 분야 없을 경우 에러 메시지("지원할 분야를 선택해주세요")
        String applyMajor = userApplyRequestDto.getApplyMajor();
        if (!StringUtils.hasText(applyMajor)) {
            throw new PrivateException(ErrorCode.APPLY_MAJOR_WRONG_INPUT);
        }

        // [Default] 지원 메시지 비었을 경우 Default 메시지 설정
        String message = userApplyRequestDto.getMessage();
        if (!StringUtils.hasText(message)) {
            message = "잘 부탁드립니다!";
        }

        UserApply userApply = UserApply.builder()
                .post(post)
                .user(user)
                .message(message)
                .applyMajor(applyMajor)
                .build();

        userApplyRepository.save(userApply);
    }

    // 모집 지원 취소
    @Transactional
    public void cancelApply(Long postId, UserDetailsImpl userDetails) {

        // [예외 처리] 조회하는 게시물이 존재하지 않을 경우
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PrivateException(ErrorCode.POST_NOT_FOUND)
        );

        // [예외 처리] 요청하는 유저 정보가 존재하지 않을 경우
        User user = userRepository.findByNickname(userDetails.getUser().getNickname()).orElseThrow(
                () -> new PrivateException(ErrorCode.NOT_FOUND_USER_INFO)
        );

        // [예외 처리]
//        UserApply userApply = userApplyRepository.findByUserAndPost(user, post).orElseThrow(
//                () -> new PrivateException(ErrorCode)
//        )
    }
}