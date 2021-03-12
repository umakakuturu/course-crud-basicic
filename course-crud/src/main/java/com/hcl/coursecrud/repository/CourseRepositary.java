package com.hcl.coursecrud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hcl.coursecrud.entity.Course;

public interface CourseRepositary extends JpaRepository<Course, Long> {
	
	public Course findByCourseId(long courseId);

}
