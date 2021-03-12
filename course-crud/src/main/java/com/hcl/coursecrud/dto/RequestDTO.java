package com.hcl.coursecrud.dto;

import java.util.List;

public class RequestDTO {

	private List<CourseDTO> courses;

	public List<CourseDTO> getCourses() {
		return courses;
	}

	public void setCourses(List<CourseDTO> courses) {
		this.courses = courses;
	}

}
