package com.hanghae99.finalproject.common.validator;

import com.hanghae99.finalproject.common.exception.ErrorCode;
import com.hanghae99.finalproject.common.exception.CustomException;
import com.hanghae99.finalproject.post.dto.PostDto;
import com.hanghae99.finalproject.user.dto.MajorDto;

import java.util.List;

public class PostValidator {

    public static void validateInputPost(PostDto.RequestDto requestDto) {

        String title = requestDto.getTitle();
        String content = requestDto.getContent();
        String deadline = requestDto.getDeadline();
        String region = requestDto.getRegion();
        List<MajorDto.RequestDto> majorList = requestDto.getMajorList();

        if (title.isEmpty() || content.isEmpty() || deadline.isEmpty() || region.isEmpty()) {
            throw new CustomException(ErrorCode.POST_WRONG_INPUT);
        }

        if (title.length() > 40) {
            throw new CustomException(ErrorCode.POST_TITLE_INPUT_LENGTH_ERROR);
        }

        if (content.length() > 250) {
            throw  new CustomException(ErrorCode.POST_CONTENT_INPUT_LENGTH_ERROR);
        }

        if (majorList.isEmpty()) {
            throw new CustomException(ErrorCode.POST_MAJOR_WRONG_INPUT);
        }
    }
}