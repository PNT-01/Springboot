package com.bit.springboard.controller;

import com.bit.springboard.dto.*;
import com.bit.springboard.service.BoardService;
import com.bit.springboard.service.impl.NoticeServiceImpl;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {
    private BoardService boardService;
    private final ApplicationContext applicationContext;

    @GetMapping("/free-list")
    public ModelAndView freelistView(@RequestParam Map<String, String> searchMap, Criteria cri) {
        ModelAndView mav = new ModelAndView();

        boardService = applicationContext.getBean("freeServiceImpl", BoardService.class);

        mav.addObject("freeList", boardService.findAll(searchMap, cri));
        mav.addObject("searchMap", searchMap);

        int total = boardService.findTotalCnt(searchMap);

        mav.addObject("page", new PageDto(cri, total));

        mav.setViewName("board/free-list");

        return mav;
    }


    @GetMapping("/notice-list")
    public ModelAndView noticelistView(@RequestParam Map<String, String> searchMap, Criteria cri) {
        ModelAndView mav = new ModelAndView();

        boardService = applicationContext.getBean("noticeServiceImpl", BoardService.class);

        cri.setAmount(9);

        List<Map<String, Object>> noticeList = new ArrayList<>();

        boardService.findAll(searchMap, cri).forEach(boardDto -> {
            Map<String, Object> map = new HashMap<>();

            map.put("notice", boardDto);

            List<BoardFileDto> noticeFileList = boardService.findFilesById(boardDto.getId());

            if(noticeFileList.size() > 0)
                map.put("file", noticeFileList.get(0));

            noticeList.add(map);
        });

//        mav.addObject("noticeList", boardService.findAll(searchMap, cri));
        mav.addObject("noticeList", noticeList);

        int total = boardService.findTotalCnt(searchMap);

        mav.addObject("page", new PageDto(cri, total));

        mav.setViewName("board/notice-list");

        return mav;
    }

//    @GetMapping("/notice-detail")
//    public ModelAndView noticedetailView() {
//        ModelAndView mav = new ModelAndView();
//
//        mav.setViewName("board/notice-detail");
//
//        return mav;
//    }

    @GetMapping("/post")
    public ModelAndView postView(HttpSession session,
                                 HttpServletResponse response) {
        ModelAndView mav = new ModelAndView();

        MemberDto loginMember = (MemberDto)session.getAttribute("loginMember");

        try {
            System.out.println(session);
            if (loginMember != null)
                mav.setViewName("board/post");
            else {
                mav.setViewName("member/login");
                response.sendRedirect("/member/login");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return mav;
    }

    @PostMapping
    // 주소를 따로 안줘도 자동으로 PostMapping 실행
    public ResponseEntity<?> post(BoardDto boardDto, MultipartFile[] uploadFiles) {
        ResponseDto<BoardDto> responseDto = new ResponseDto<>();

        if("free".equals(boardDto.getType())) { // 어떻게 하는지 확인
            boardService = applicationContext.getBean("freeServiceImpl", BoardService.class);
        } else {
            boardService = applicationContext.getBean("noticeServiceImpl", BoardService.class);
        }

        try {
            BoardDto returnBoardDto = boardService.post(boardDto, uploadFiles);

            responseDto.setStatusCode(201);
            responseDto.setStatusMessage("created");
            responseDto.setData(returnBoardDto);

            return ResponseEntity.created(URI.create("/boards")).body(responseDto);
        } catch(Exception e) {
            responseDto.setStatusCode(500);
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);

        }
    }

    @GetMapping("/{id}")
    public ModelAndView boardDetail (@Parameter(description = "조회하고 싶은 글의 ID")
                                     @PathVariable("id") int id,
                                     @Parameter(description = "게시판 타입")
                                     @RequestParam("type") String type) {
           ModelAndView mav = new ModelAndView();

           if(type.equals("free")) {
               boardService = applicationContext.getBean("freeServiceImpl", BoardService.class);
               mav.setViewName("board/free-detail");
           } else {
               boardService = applicationContext.getBean("noticeServiceImpl", BoardService.class);
               mav.setViewName("board/notice-detail");
           }

           mav.addObject("board", boardService.findById(id));
           mav.addObject("fileList", boardService.findFilesById(id));

           return mav;
    }

    @PatchMapping("{id}")
    public ResponseEntity<?> modify(@PathVariable("id") int id,
                                    BoardDto boardDto,
                                    MultipartFile[] uploadFiles,
                                    MultipartFile[] changeFiles,
                                    @RequestParam(name = "originFiles", required = false) String originFiles) {
        ResponseDto<BoardDto> responseDto = new ResponseDto<>();

        if(boardDto.getType().equals("free")) {
            boardService = applicationContext.getBean("freeServiceImpl", BoardService.class);
        } else {
            boardService = applicationContext.getBean("noticeServiceImpl", BoardService.class);
        }

        try {
            boardDto.setId(id);
            BoardDto modifiedBoardDto = boardService.modify(boardDto, uploadFiles, changeFiles, originFiles);

            responseDto.setStatusCode(200);
            responseDto.setStatusMessage("OK");
            responseDto.setData(modifiedBoardDto);

            return ResponseEntity.ok(responseDto);
        } catch(Exception e) {
            responseDto.setStatusCode(500);
            responseDto.setStatusMessage(e.getMessage());
            return ResponseEntity.internalServerError().body(responseDto);
        }
    }

    @GetMapping("/cnt/{id}")
    public void updateBoardCnt(@PathVariable("id") int id,
                               @RequestParam("type") String type,
                               HttpServletResponse response) {
        if(type.equals("free")) {
            boardService = applicationContext.getBean("freeServiceImpl", BoardService.class);
        } else {
            boardService = applicationContext.getBean("noticeServiceImpl", BoardService.class);
        }

        boardService.updateBoardCnt(id);

        try {
            response.sendRedirect("/boards/" + id + "?type=" + type);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<?> remove(@PathVariable("id") int id,
                                    @RequestParam("type") String type) {

        if(type.equals("free")) {
            boardService = applicationContext.getBean("freeServiceImpl", BoardService.class);
        } else {
            boardService = applicationContext.getBean("noticeServiceImpl", BoardService.class);
        }

        try {
            boardService.remove(id);

            return ResponseEntity.noContent().build();
        } catch(Exception e) {
            ResponseDto<BoardDto> responseDto = new ResponseDto<>();

            responseDto.setStatusCode(500);
            responseDto.setStatusMessage(e.getMessage());

            return ResponseEntity.internalServerError().body(responseDto);

        }
    }
}
