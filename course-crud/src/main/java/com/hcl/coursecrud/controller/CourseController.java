package com.hcl.coursecrud.controller;

//import org.omg.CORBA.portable.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.hcl.coursecrud.dto.CourseDTO;
import com.hcl.coursecrud.dto.ResponseDTO;
import com.hcl.coursecrud.entity.Course;
import com.hcl.coursecrud.service.CourseService;

@RestController
public class CourseController {

	@Autowired
	CourseService courseService;

	@PostMapping("/addCourse")
	public ResponseDTO registerCourse(@RequestBody CourseDTO courseDTO) {
		return courseService.registerCourse(courseDTO);
	}

	@GetMapping("/getCourses")
	public ResponseDTO getCourses() {
		return courseService.getCourses();
	}

	@GetMapping("/coursesById/{id}")
	public ResponseEntity<CourseDTO> getCoursesById(@PathVariable(name = "id") long id) {

		CourseDTO course = courseService.getCourseById(id);
		if (course != null) {
			return new ResponseEntity<CourseDTO>(course, HttpStatus.OK);
		} else {
			return new ResponseEntity<CourseDTO>(course, HttpStatus.BAD_REQUEST);
		}

	}

	/*
	 * @PostMapping("/coursesByIds") public ResponseDTO
	 * validateCourseIds(@RequestBody RequestDTO reqDtos) throws
	 * ApplicationException {
	 * 
	 * return courseService.validateCourseIds(reqDtos);
	 * 
	 * }
	 */

	@DeleteMapping("/deleteCourse/{courseId}")
	private ResponseEntity<String> deleteCourse(@PathVariable("courseId") long courseId) {
		return new ResponseEntity<String>(courseService.delete(courseId), HttpStatus.OK);
	}

	@PutMapping("/updatetheCourse/{courseId}")
	public ResponseEntity<Course> updateCourse(@RequestBody Course course, @PathVariable long courseId) {
		return new ResponseEntity<Course>(courseService.updateCourse(course), HttpStatus.OK);

	}

}
