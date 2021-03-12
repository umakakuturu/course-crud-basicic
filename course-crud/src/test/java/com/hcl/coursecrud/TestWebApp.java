package com.hcl.coursecrud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.aspectj.lang.annotation.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class TestWebApp extends CourseCrudApplicationTests {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@Before(value = "")
	public void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void testCourse() throws Exception {
		mockMvc.perform(get("/course")).andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(jsonPath("$.courseName").value("java"))
				.andExpect(jsonPath("$.courseDescription").value("java is a prgrming lang."));

		/*
		 * (get("/employee")).andExpect(status().isOk())
		 * .andExpect(content().contentType("application/json;charset=UTF-8"))
		 * .andExpect(jsonPath("$.name").value("emp1")).andExpect(jsonPath(
		 * "$.designation").value("manager"))
		 * .andExpect(jsonPath("$.empId").value("1")).andExpect(jsonPath("$.salary").
		 * value(3000));
		 */

	}

}
