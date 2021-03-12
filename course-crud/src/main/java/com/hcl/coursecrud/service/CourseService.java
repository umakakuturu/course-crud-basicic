package com.hcl.coursecrud.service;

import org.springframework.stereotype.Service;

import com.hcl.coursecrud.dto.CourseDTO;
import com.hcl.coursecrud.dto.RequestDTO;
import com.hcl.coursecrud.dto.ResponseDTO;
import com.hcl.coursecrud.entity.Course;

@Service
public interface CourseService {

	public ResponseDTO registerCourse(CourseDTO courseDTO);

	public ResponseDTO getCourses();

	public CourseDTO getCourseById(Long courseId);

	public ResponseDTO validateCourseIds(RequestDTO courseDtos);
	
	public String delete(Long courseId);
	
	public Course updateCourse(Course course);

}
