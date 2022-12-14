package com.ssafy.spring.member.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ssafy.spring.member.model.UserDto;
import com.ssafy.spring.member.model.service.JwtService;
import com.ssafy.spring.member.model.service.UserService;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Controller
@RequestMapping("/user")
public class UserRestController {

	private final Logger logger = LoggerFactory.getLogger(UserRestController.class);
	private static final String SUCCESS="success";
	private static final String FAIL = "fail";
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UserService userService;
	
	@ExceptionHandler
	public ResponseEntity<String> handler(Exception e){
		logger.error("ErrorHandler.............................");
		logger.error("ErrorMessage.............................{}",e.getMessage());
		e.printStackTrace();
		
		HttpHeaders resHeaders = new HttpHeaders();
		resHeaders.add("Content-Type", "application/json;charset=UTF-8");
		if (e instanceof Exception) {
			return new ResponseEntity<String>(e.getMessage(), resHeaders, HttpStatus.FAILED_DEPENDENCY);
		}else {
			return new ResponseEntity<String>("?????? ??? ?????? ??????", resHeaders, HttpStatus.FAILED_DEPENDENCY);
		}
	}
	
	/** ?????? ?????? ?????? */
	
	@ApiOperation(value="???????????? ??????", notes = "?????? ?????? ???????????? ???????????? ?????? ????????? ??????")
	@PostMapping("/register")
	public ResponseEntity<String> regist(@RequestBody UserDto user) {
		logger.debug("user regist..............................{}",user);
		userService.userRegister(user);
		return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
	}
	
	@ApiOperation(value="???????????? ??????", notes = "?????? ?????? ???????????? ???????????? ?????? ????????? ??????")
	@PutMapping("/update")
	public ResponseEntity<String> udpate(@RequestBody UserDto user) {
		logger.debug("user update....................{}", user);
		userService.userUpdate(user);
		return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
	}
	
	@ApiOperation(value="???????????? ??????", notes = "id??? ???????????? ??????????????? ??????")
	@DeleteMapping("remove/{id}")
	public ResponseEntity<String> delete(@PathVariable String id) {
		logger.debug("user delete....................id:{}", id);
		userService.userDelete(id);
		return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
	}
	
	/** ????????? Token*/
	
	@ApiOperation(value = "?????????", notes = "Access-token??? ????????? ?????? ???????????? ????????????.", response = Map.class)
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(
			@RequestBody @ApiParam(value = "????????? ??? ????????? ????????????(?????????, ????????????).", required = true) UserDto user) {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;
		logger.debug("???????????? ????????????? {}", user);
		try {
			UserDto loginUser = userService.login(user);
			logger.debug("login ????????????? {}", loginUser);
			if (loginUser != null) {
				String accessToken = jwtService.createAccessToken("id", loginUser.getId());// key, data
				String refreshToken = jwtService.createRefreshToken("id", loginUser.getId());// key, data
				userService.saveRefreshToken(user.getId(), refreshToken);
				logger.debug("????????? accessToken ?????? : {}", accessToken);
				logger.debug("????????? refreshToken ?????? : {}", refreshToken);
				resultMap.put("access-token", accessToken);
				resultMap.put("refresh-token", refreshToken);
				resultMap.put("message", SUCCESS);
				status = HttpStatus.ACCEPTED;
			} else {
				resultMap.put("message", FAIL);
				status = HttpStatus.ACCEPTED;
			}
		} catch (Exception e) {
			logger.error("????????? ?????? : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<Map<String, Object>>(resultMap, status);
	}
	
	@ApiOperation(value = "????????????", notes = "?????? ????????? ?????? Token??? ????????????.", response = Map.class)
	@GetMapping("/info/{id}")
	public ResponseEntity<Map<String, Object>> getInfo(
			@PathVariable("id") @ApiParam(value = "????????? ????????? ?????????.", required = true) String id,
			HttpServletRequest request) {
		logger.debug("getInfo id : {} ", id);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = HttpStatus.UNAUTHORIZED;
		logger.debug("getInfo userInfo id : {}", id);
		if (jwtService.checkToken(request.getHeader("access-token"))) {
			logger.info("?????? ????????? ??????!!!");
			try {
//				????????? ????????? ??????.
				UserDto userDto = userService.userInfo(id);
				logger.debug("userInfo userDto : {}", userDto);
				resultMap.put("userInfo", userDto);
				resultMap.put("message", SUCCESS);
				status = HttpStatus.ACCEPTED;
			} catch (Exception e) {
				logger.error("???????????? ?????? : {}", e);
				resultMap.put("message", e.getMessage());
				status = HttpStatus.INTERNAL_SERVER_ERROR;
			}
		} else {
			logger.error("?????? ????????? ??????!!!");
			resultMap.put("message", FAIL);
			status = HttpStatus.UNAUTHORIZED;
		}
		return new ResponseEntity<Map<String, Object>>(resultMap, status);
	}
	
	@ApiOperation(value = "????????????", notes = "?????? ????????? ?????? Token??? ????????????.", response = Map.class)
	@GetMapping("/logout/{id}")
	public ResponseEntity<?> removeToken(@PathVariable("id") String id) {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = HttpStatus.ACCEPTED;
		try {
			userService.deleteRefreshToken(id);
			resultMap.put("message", SUCCESS);
			status = HttpStatus.ACCEPTED;
		} catch (Exception e) {
			logger.error("???????????? ?????? : {}", e);
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return new ResponseEntity<Map<String, Object>>(resultMap, status);
	}
	
	@ApiOperation(value = "Access Token ?????????", notes = "????????? access token??? ??????????????????.", response = Map.class)
	@PostMapping("/refresh")
	public ResponseEntity<?> refreshToken(@RequestBody UserDto user, HttpServletRequest request)
			throws Exception {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = HttpStatus.ACCEPTED;
		String token = request.getHeader("refresh-token");
		logger.debug("token : {}, userDto : {}", token, user);
		if (jwtService.checkToken(token)) {
			if (token.equals(userService.getRefreshToken(user.getId()))) {
				String accessToken = jwtService.createAccessToken("id", user.getId());
				logger.debug("token : {}", accessToken);
				logger.debug("??????????????? ??????????????? ?????????!!!");
				resultMap.put("access-token", accessToken);
				resultMap.put("message", SUCCESS);
				status = HttpStatus.ACCEPTED;
			}
		} else {
			logger.debug("????????????????????? ?????????!!!!!!!");
			status = HttpStatus.UNAUTHORIZED;
		}
		return new ResponseEntity<Map<String, Object>>(resultMap, status);
	}
}
