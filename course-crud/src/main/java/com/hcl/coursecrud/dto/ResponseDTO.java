package com.hcl.coursecrud.dto;

import java.util.List;

public class ResponseDTO {

	private Integer statusCode;
	private String Message;

	private List<CourseDTO> courseDTOs;

	public Integer getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}

	public String getMessage() {
		return Message;
	}

	public void setMessage(String message) {
		Message = message;
	}

	public List<CourseDTO> getCourseDTOs() {
		return courseDTOs;
	}

	public void setCourseDTOs(List<CourseDTO> courseDTOs) {
		this.courseDTOs = courseDTOs;
	}

}
