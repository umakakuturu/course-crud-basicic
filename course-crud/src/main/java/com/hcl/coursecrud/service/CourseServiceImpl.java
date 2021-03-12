package com.hcl.coursecrud.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.hcl.coursecrud.dto.CourseDTO;
import com.hcl.coursecrud.dto.RequestDTO;
import com.hcl.coursecrud.dto.ResponseDTO;
import com.hcl.coursecrud.entity.Course;
import com.hcl.coursecrud.repository.CourseRepositary;

@Service
public class CourseServiceImpl implements CourseService {

	@Autowired
	CourseRepositary courseRepositary;

	@Override
	public ResponseDTO registerCourse(CourseDTO courseDTO) {
		ResponseDTO responseDTO = new ResponseDTO();
		if (courseDTO == null) {
			responseDTO.setMessage("courseDTO param is null or empty");
			responseDTO.setStatusCode(HttpStatus.BAD_REQUEST.value());
		} else {

			Course course = new Course();
			BeanUtils.copyProperties(courseDTO, course);
			courseRepositary.save(course);

			responseDTO.setMessage("Course is successfully added");
			responseDTO.setStatusCode(HttpStatus.OK.value());

		}

		return responseDTO;
	}

	@Override
	public ResponseDTO getCourses() {
		ResponseDTO responseDTO = new ResponseDTO();
		try {
			List<Course> courses = courseRepositary.findAll();
			List<CourseDTO> courseDTOs = new ArrayList<CourseDTO>();
			for (Course course : courses) {
				CourseDTO courseDTO = new CourseDTO();
				BeanUtils.copyProperties(course, courseDTO);
				courseDTOs.add(courseDTO);
			}

			responseDTO.setCourseDTOs(courseDTOs);
			responseDTO.setMessage("Success");
			responseDTO.setStatusCode(HttpStatus.OK.value());
		} catch (Exception e) {
			responseDTO.setMessage("Failed");
			responseDTO.setStatusCode(HttpStatus.EXPECTATION_FAILED.value());
		}
		return responseDTO;
	}

	@Override
	public CourseDTO getCourseById(Long courseId) {
		if (courseId == null) {

		}
		CourseDTO courseDTO = new CourseDTO();
		Course Course = new Course();

		Course = courseRepositary.findByCourseId(courseId);

		BeanUtils.copyProperties(Course, courseDTO);

		return courseDTO;
	}

	@Override
	public ResponseDTO validateCourseIds(RequestDTO reqDtos) {

		ResponseDTO dto = new ResponseDTO();
		try {
			if (null == reqDtos)
				throw new Exception("courseDtos param is null or empty");

			List<CourseDTO> courseDto = new ArrayList<>();

			for (CourseDTO courseDTO : reqDtos.getCourses()) {
				Long courseId = courseDTO.getCourseId();
				Course tCourse = courseRepositary.findByCourseId(courseId);
				if (tCourse == null) {
					dto.setMessage("Failed, Course Ids are not present in the system.");
					dto.setStatusCode(HttpStatus.BAD_REQUEST.value());
					break;
				}

				CourseDTO cdto = new CourseDTO();
				BeanUtils.copyProperties(tCourse, cdto);
				courseDto.add(cdto);
			}

			dto.setCourseDTOs(courseDto);
			dto.setMessage("Success");
			dto.setStatusCode(HttpStatus.OK.value());

		} catch (Exception e) {
			dto.setMessage("Failed");
			dto.setStatusCode(HttpStatus.BAD_REQUEST.value());
		}

		return dto;
	}

	@Override
	public Course updateCourse(Course course) {
		// TODO Auto-generated method stub
		return courseRepositary.save(course);
	}

	@Override
	public String delete(Long courseId) {
		// TODO Auto-generated method stub
		if ("Optional.empty".contains("" + courseRepositary.findById(courseId))) {
			return "courseId deleted successfully";
		} else {
			courseRepositary.deleteById(courseId);
			return "courseId deleted successfully";

		}
	}

}
