package com.hanghae99.finalprooject.service;

import com.hanghae99.finalprooject.dto.PostDto;
import com.hanghae99.finalprooject.exception.ErrorCode;
import com.hanghae99.finalprooject.exception.PrivateException;
import com.hanghae99.finalprooject.model.CurrentStatus;
import com.hanghae99.finalprooject.model.Img;
import com.hanghae99.finalprooject.model.Post;
import com.hanghae99.finalprooject.model.User;
import com.hanghae99.finalprooject.repository.ImgRepository;
import com.hanghae99.finalprooject.repository.PostRepository;
import com.hanghae99.finalprooject.repository.UserRepository;
import com.hanghae99.finalprooject.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImgRepository imgRepository;

    private final AwsS3UploadService awsS3UploadService;

    // post 등록
    @Transactional
    public void createPost(PostDto.RequestDto requestDto, List<String> imgUrlList, UserDetailsImpl userDetails) {

        // 일단 post 작성이 이미지 필수 등록으로 설정....(회의해봐야함)
        postBlankCheck(imgUrlList);

        User user = userDetails.getUser();

        String title = requestDto.getTitle();
        String content = requestDto.getContent();
        String deadline = requestDto.getDeadline();
        CurrentStatus currentStatus = requestDto.getCurrentStatus();
        String region = requestDto.getRegion();
        String category = requestDto.getCategory();

        Post post = new Post(title, content, deadline, currentStatus, region, category, user);

        List<String> imgList = new ArrayList<>();
        for (String imgUrl : imgUrlList) {
            Img img = new Img(imgUrl, post);
            imgRepository.save(img);
            imgList.add(img.getImgUrl());
        }
        postRepository.save(post);
    }

    private void postBlankCheck(List<String> imgPaths) {
        if(imgPaths == null || imgPaths.isEmpty()){
            throw new PrivateException(ErrorCode.WRONG_INPUT_IMAGE);
        }
    }

    public PostDto.DetailDto getDetail(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PrivateException(ErrorCode.POST_NOT_FOUND)
        );

        List<String> imgUrl = imgRepository.findAllByPost(post)
                .stream()
                .map(Img::getImgUrl)
                .collect(Collectors.toList());

        // 댓글 추가 필요

        return new PostDto.DetailDto(postId, post, imgUrl);
    }

    @Transactional
    public void updatePost(Long postId, PostDto.RequestDto requestDto, List<MultipartFile> imgUrlList, UserDetailsImpl userDetails) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PrivateException(ErrorCode.POST_NOT_FOUND)
        );

        User user = userRepository.findByNickname(userDetails.getUser().getNickname()).orElseThrow(
                () -> new PrivateException(ErrorCode.NOT_FOUND_USER_INFO)
        );

        // 본인 post만 수정
        if (!post.getUser().equals(user)) {
            throw new PrivateException(ErrorCode.POST_UPDATE_WRONG_ACCESS);
        }

        List<String> imgPaths = awsS3UploadService.updateImg(postId, imgUrlList);
        log.info("수정된 이미지 Url : " + imgPaths);

        post.updatePost(requestDto);

        List<String> imgList = new ArrayList<>();
        for (String imgUrl : imgPaths) {
            Img img = new Img(imgUrl, post);
            imgRepository.save(img);
            imgList.add(img.getImgUrl());
        }
    }

    // post 삭제
    @Transactional
    public void deletePost(Long postId, UserDetailsImpl userDetails) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PrivateException(ErrorCode.POST_NOT_FOUND)
        );

        User user = userRepository.findByNickname(userDetails.getUser().getNickname()).orElseThrow(
                () -> new PrivateException(ErrorCode.NOT_FOUND_USER_INFO)
        );

        if (!post.getUser().equals(user)) {
            throw new PrivateException(ErrorCode.POST_UPDATE_WRONG_ACCESS);
        }
        awsS3UploadService.deleteImg(postId);
        imgRepository.deleteAllByPostId(postId);
        postRepository.deleteById(postId);
    }
}