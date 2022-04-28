package com.hanghae99.finalprooject.service;

import com.hanghae99.finalprooject.dto.PostDto;
import com.hanghae99.finalprooject.exception.ErrorCode;
import com.hanghae99.finalprooject.exception.PrivateException;
import com.hanghae99.finalprooject.model.CurrentStatus;
import com.hanghae99.finalprooject.repository.ImgRepository;
import com.hanghae99.finalprooject.repository.PostRepository;
import com.hanghae99.finalprooject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ImgRepository imgRepository;

    // post 등록
    @Transactional
    public void createPost(PostDto.RequestDto requestDto, List<String> imgUrlList) {

        // 일단 post 작성이 이미지 필수 등록으로 설정....(회의해봐야함)
        postBlankCheck(imgUrlList);

//        User user = userRepository.findByNickname(username).orElseThrow(
//                () -> new PrivateException(ErrorCode.NOT_FOUND_USER_INFO)
//        );

        String title = requestDto.getTitle();
        String content = requestDto.getContent();
        String deadline = requestDto.getDeadline();
        CurrentStatus status = requestDto.getCurrentStatus();
        String region = requestDto.getRegion();
        String category = requestDto.getCategory();

//        Post post = new Post(title, content, deadline, status, region, category);

//        List<String> imgList = new ArrayList<>();
//        for (String imgUrl : imgUrlList) {
//            Img img = new Img(imgUrl, post);
//            imgRepository.save(img);
//            imgList.add(img.getImgUrl());
//        }
//        postRepository.save(post);
    }

    private void postBlankCheck(List<String> imgPaths) {
        if(imgPaths == null || imgPaths.isEmpty()){
            throw new PrivateException(ErrorCode.WRONG_INPUT_IMAGE);
        }
    }
}