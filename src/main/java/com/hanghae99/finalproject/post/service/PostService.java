package com.hanghae99.finalproject.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanghae99.finalproject.comment.dto.CommentResponseDto;
import com.hanghae99.finalproject.comment.model.Comment;
import com.hanghae99.finalproject.comment.repository.CommentRepository;
import com.hanghae99.finalproject.common.exception.CustomException;
import com.hanghae99.finalproject.common.exception.ErrorCode;
import com.hanghae99.finalproject.img.dto.ImgDto;
import com.hanghae99.finalproject.img.dto.ImgUrlDto;
import com.hanghae99.finalproject.img.model.Img;
import com.hanghae99.finalproject.img.repository.ImgRepository;
import com.hanghae99.finalproject.img.service.AwsS3UploadService;
import com.hanghae99.finalproject.img.service.FileUploadService;
import com.hanghae99.finalproject.post.dto.PostDto;
import com.hanghae99.finalproject.post.model.CurrentStatus;
import com.hanghae99.finalproject.post.model.Post;
import com.hanghae99.finalproject.post.repository.PostRepository;
import com.hanghae99.finalproject.security.UserDetailsImpl;
import com.hanghae99.finalproject.user.dto.MajorDto;
import com.hanghae99.finalproject.user.model.Major;
import com.hanghae99.finalproject.user.model.User;
import com.hanghae99.finalproject.user.model.UserApply;
import com.hanghae99.finalproject.user.model.UserStatus;
import com.hanghae99.finalproject.user.repository.MajorRepository;
import com.hanghae99.finalproject.user.repository.UserApplyRepository;
import com.hanghae99.finalproject.user.repository.UserRateRepository;
import com.hanghae99.finalproject.user.repository.UserRepository;
import com.hanghae99.finalproject.user.service.UserApplyService;
import com.hanghae99.finalproject.common.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ImgRepository imgRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final MajorRepository majorRepository;
    private final UserApplyRepository userApplyRepository;
    private final UserRateRepository userRateRepository;

    private final FileUploadService fileUploadService;
    private final AwsS3UploadService s3UploadService;
    private final UserApplyService userApplyService;

    // post ??????
    @Transactional
    public void createPost(String jsonString, List<MultipartFile> imgs, UserDetailsImpl userDetails) throws IOException {
        log.info("multipartFile imgs={}", imgs);
        List<Img> imgList = new ArrayList<>();
        List<Major> majorList = new ArrayList<>();

        List<ImgDto> imgDtoList = new ArrayList<>();

        if (imgs != null) {
            for (MultipartFile img : imgs) {
                ImgDto imgDto = fileUploadService.uploadImage(img, "post");
                imgDtoList.add(imgDto);
            }
        }

        // ???????????? String??? jsonString??? Dto??? ??????
        ObjectMapper objectMapper = new ObjectMapper();
        PostDto.RequestDto requestDto = objectMapper.readValue(jsonString, PostDto.RequestDto.class);

        User user = loadUserByUserId(userDetails);
        dtoParser(imgList, imgDtoList, majorList, requestDto);

        // [????????? ??????] ??????, ??????, ??????, ??????, ?????? ?????? ?????? ??????
        PostValidator.validateInputPost(requestDto);

        Post post = Post.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .deadline(requestDto.getDeadline())
                .currentStatus(CurrentStatus.ONGOING)
                .region(requestDto.getRegion())
                .link(requestDto.getLink())
                .imgList(imgList)
                .majorList(majorList)
                .user(user)
                .build();
        postRepository.save(post);
    }

    // post ?????? ??????
    @Transactional
    public PostDto.DetailDto getDetail(Long postId, UserDetailsImpl userDetails) {
        // postId??? ???????????? ?????????
        Post post = loadPostByPostId(postId);
        
        /*
        1) ?????? ??? ??? ????????? ????????? ????????? ?????? ?????? : userStatus - starter
        2) ?????? ????????? ????????? ????????? ????????? ?????? ?????? : userStatus - applicant
        3) ?????? ?????? ??? ????????? ????????? ????????? ????????? ?????? ?????? : userStatus - member
        4) ????????? ??????(?????????, ????????? ??????) : userStatus - user
        5) ??????????????? ?????? ?????? : userStatus - anonymous
         */
        String userStatus;
        if (userDetails != null) {
            User user = userDetails.getUser();
            if (userApplyService.isStarter(post, user)) {   // 1)
                userStatus = UserStatus.USER_STATUS_TEAM_STARTER.getUserStatus();
            } else if (isMember(user, post) == 0) {   // 2)
                userStatus = UserStatus.USER_STATUS_APPLICANT.getUserStatus();
            } else if (isMember(user, post) == 1) { // 3
                userStatus = UserStatus.USER_STATUS_MEMBER.getUserStatus();
            } else {    // 4)
                userStatus = UserStatus.USER_STATUS_USER.getUserStatus();
            }
        } else {    // 5)
            userStatus = UserStatus.USER_STATUS_ANONYMOUS.getUserStatus();
        }

        // imgList
        List<String> imgUrl = imgRepository.findAllByPost(post)
                .stream()
                .map(Img::getImgUrl)
                .collect(Collectors.toList());

        // commentList
        List<Comment> findCommentByPost = commentRepository.findAllByPost(post);
        List<CommentResponseDto> commentList = new ArrayList<>();
        for (Comment comment : findCommentByPost) {
            commentList.add(new CommentResponseDto(
                    comment
                    ));
        }

        // majortList
        List<Major> findMajorByPost = majorRepository.findAllByPost(post);

        List<MajorDto.ResponseDto> majorList = new ArrayList<>();
        for (Major major : findMajorByPost) {
            majorList.add(new MajorDto.ResponseDto(major));
        }
        return new PostDto.DetailDto(userStatus, postId, post, imgUrl, commentList, majorList);
    }

    // post ??????
    @Transactional
    public void editPost(Long postId, String jsonString, List<MultipartFile> imgs, UserDetailsImpl userDetails) throws IOException {
        log.info("?????? ??????={}", jsonString);

        // ???????????? String??? jsonString??? Dto??? ??????
        ObjectMapper objectMapper = new ObjectMapper();
        PostDto.PutRequestDto putRequestDto = objectMapper.readValue(jsonString, PostDto.PutRequestDto.class);

        // postId??? ???????????? ?????????
        Post post = loadPostByPostId(postId);

        // ???????????? ??????
        User user = loadUserByUserId(userDetails);

        // ?????? post??? ?????? ??????
        if (!post.getUser().equals(user)) {
            throw new CustomException(ErrorCode.POST_UPDATE_WRONG_ACCESS);
        }

        List<Img> imgList = post.getImgList();
        List<ImgDto> imgDtoList = new ArrayList<>();
        List<Img> removeImgList = new ArrayList<>();

        // ????????? ????????? S3, ????????? DB?????? ????????????
        for (Img img : imgList) {
            for (ImgUrlDto imgUrlDto : putRequestDto.getImgUrl()) {
                if (img.getImgUrl().equals(imgUrlDto.getImgUrl())) {
                    s3UploadService.deleteFile(img.getImgName());
                    s3UploadService.deleteFile(img.getImgName().replace("post/", "post-resized/"));
                    imgRepository.deleteById(img.getId());
                    // removeImgList??? ????????? ????????? ??????
                    removeImgList.add(img);
                }
            }
        }

        // / removeImgList??? ?????? ?????? ????????? ?????? Imglist?????? ??????
        for (Img img : removeImgList) {
            imgList.remove(img);
        }

        // ????????? ????????? S3??? ??????
        if (imgs != null) {
            for (MultipartFile img : imgs) {
                if(!img.isEmpty()) {
                    ImgDto imgDto = fileUploadService.uploadImage(img, "post");
                    imgDtoList.add(imgDto);
                }
            }
        }
        putDtoParser(imgList, imgDtoList);
        post.updatePost(putRequestDto, imgList);
    }

    // post ??????
    @Transactional
    public void deletePost(Long postId, UserDetailsImpl userDetails) {
        // postId??? ???????????? ?????????
        Post post = loadPostByPostId(postId);

        // ???????????? ??????
        User user = loadUserByUserId(userDetails);

        // ?????? post??? ?????? ??????
        if (!post.getUser().equals(user)) {
            throw new CustomException(ErrorCode.POST_DELETE_WRONG_ACCESS);
        }

        // post ????????? s3??? ????????? ???????????? ??????
        List<Img> imgList = imgRepository.findAllByPost(post);
        for (Img img : imgList) {
            s3UploadService.deleteFile(img.getImgName());
            s3UploadService.deleteFile(img.getImgName().replace("post/", "post-resized/"));
        }

        // post ????????? ?????? ????????? ??????
        userRateRepository.deleteAllByPostId(postId);

        postRepository.deleteById(postId);
    }

    // [?????? ??????] postId??? ???????????? ????????? ?????? ??????
    private Post loadPostByPostId(Long PostId) {
        return postRepository.findById(PostId).orElseThrow(
                () -> new CustomException(ErrorCode.POST_NOT_FOUND)
        );
    }

    // [?????? ??????] ???????????? ?????? ????????? ???????????? ?????? ??????
    private User loadUserByUserId(UserDetailsImpl userDetails) {
        return  userRepository.findById(userDetails.getUser().getId()).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER_INFO)
        );
    }

    private void dtoParser(List<Img> imgList, List<ImgDto> imgDtoList, List<Major> majorList, PostDto.RequestDto requestDto) {
        for (ImgDto imgDto : imgDtoList) {
            Img img = Img.builder()
                    .imgName(imgDto.getImgName())
                    .imgUrl(imgDto.getImgUrl())
                    .build();
            imgList.add(img);
        }

        for (MajorDto.RequestDto majorRequestDto : requestDto.getMajorList()) {
            Major major = Major.builder()
                    .majorName(majorRequestDto.getMajorName())
                    .numOfPeopleSet(majorRequestDto.getNumOfPeopleSet())
                    .numOfPeopleApply(0)
                    .build();
            majorList.add(major);
        }
    }

    /*
    ????????? ????????? ?????? ?????? isAccepted = -1
    ????????? ????????? ?????? ?????? isAccepted = userApply.getIsAccepted
    userApply.getIsAccepted = 0 : ?????? ?????? ??????
    userApply.getIsAccepted = 1 : ?????? ?????? ??? ????????? ??????
     */
    private int isMember(User user, Post post) {
        Optional<UserApply> userApplyOptional = userApplyRepository.findUserApplyByUserAndPost(user, post);
        int isAccepted;
        isAccepted = userApplyOptional.map(UserApply::getIsAccepted).orElse(-1);
        return isAccepted;
    }

    private void putDtoParser(List<Img> imgList, List<ImgDto> imgDtoList) {
        for (ImgDto imgDto : imgDtoList) {
            Img img = Img.builder()
                    .imgName(imgDto.getImgName())
                    .imgUrl(imgDto.getImgUrl())
                    .build();
            imgList.add(img);
        }
    }
}