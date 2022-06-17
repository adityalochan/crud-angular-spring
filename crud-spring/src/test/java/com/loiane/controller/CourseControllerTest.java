package com.loiane.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loiane.TestData;
import com.loiane.ValidationAdvice;
import com.loiane.exception.RecordNotFoundException;
import com.loiane.model.Course;
import com.loiane.service.CourseService;

@ActiveProfiles("test")
@ContextConfiguration(classes = { CourseController.class })
@ExtendWith(SpringExtension.class)
public class CourseControllerTest {

    private final static String API = "/api/courses";
    private final static String API_ID = "/api/courses/{id}";

    @Autowired
    private CourseController courseController;

    @MockBean
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        ProxyFactory factory = new ProxyFactory(new CourseController(courseService));
        factory.addAdvice(new ValidationAdvice());
        courseController = (CourseController) factory.getProxy();
    }

    /**
     * Method under test: {@link CourseController#findAll()}
     */
    @Test
    @DisplayName("Should return a list of courses in JSON format")
    void testFindAll() throws Exception {
        Course course = TestData.createValidCourse();
        List<Course> courses = List.of(course);
        when(this.courseService.findAll()).thenReturn(courses);
        MockMvcBuilders.standaloneSetup(this.courseController)
                .build()
                .perform(MockMvcRequestBuilders.get(API))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(courses.size())))
                .andExpect(jsonPath("$[0]._id", is(course.getId()), Long.class))
                .andExpect(jsonPath("$[0].name", is(course.getName())))
                .andExpect(jsonPath("$[0].category", is(course.getCategory())));
    }

    /**
     * Method under test: {@link CourseController#findById(Long)}
     */
    @Test
    @DisplayName("Should return a course by id")
    void testFindById() throws Exception {
        Course course = TestData.createValidCourse();
        when(this.courseService.findById(anyLong())).thenReturn(course);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(API_ID, course.getId());
        MockMvcBuilders.standaloneSetup(this.courseController)
                .build()
                .perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("_id", is(course.getId()), Long.class))
                .andExpect(jsonPath("name", is(course.getName())))
                .andExpect(jsonPath("category", is(course.getCategory())));
    }

    /**
     * Method under test: {@link CourseController#findById(Long)}
     */
    @Test
    @DisplayName("Should return a 404 status code when course is not found")
    void testFindByIdNotFound() throws Exception {
        when(this.courseService.findById(anyLong())).thenThrow(new RecordNotFoundException(123L));
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(API_ID, 1);
        assertThrows(NestedServletException.class, () -> {
            ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(this.courseController)
                    .build()
                    .perform(requestBuilder);
            actualPerformResult.andExpect(status().isNotFound());
        });
    }

    /**
     * Method under test: {@link CourseController#findById(Long)}
     */
    @Test
    @DisplayName("Should return bad request status code when id is not a positive number")
    void testFindByIdNegative() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(API_ID, -1);
        assertThrows(NestedServletException.class, () -> {
            ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(this.courseController)
                    .build()
                    .perform(requestBuilder);
            actualPerformResult.andExpect(status().isBadRequest());
        });
    }

    /**
     * Method under test: {@link CourseController#create(Course)}
     */
    @Test
    @DisplayName("Should create a course when valid")
    void testCreate() throws Exception {
        Course course = TestData.createValidCourse();
        when(this.courseService.create(course)).thenReturn(course);

        String content = (new ObjectMapper()).writeValueAsString(course);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
        MockMvcBuilders.standaloneSetup(this.courseController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("_id", is(course.getId()), Long.class))
                .andExpect(jsonPath("name", is(course.getName())))
                .andExpect(jsonPath("category", is(course.getCategory())));
    }

    /**
     * Method under test: {@link CourseController#create(Course)}
     * 
     * @throws JsonProcessingException
     */
    @Test
    @DisplayName("Should return bad request when creating an invalid course")
    void testCreateInvalid() throws Exception {
        final List<Course> courses = TestData.createInvalidCourses();
        for (Course course : courses) {
            String content = (new ObjectMapper()).writeValueAsString(course);
            MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(API)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content);
            ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(this.courseController)
                    .build()
                    .perform(requestBuilder);
            actualPerformResult.andExpect(status().isBadRequest());
        }
    }

    /**
     * Method under test: {@link CourseController#update(Long, Course)}
     */
    @Test
    @DisplayName("Should update a course when valid")
    void testUpdate() throws Exception {
        Course course = TestData.createValidCourse();
        when(this.courseService.update((Long) any(), (Course) any())).thenReturn(course);

        String content = (new ObjectMapper()).writeValueAsString(course);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(API_ID, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
        MockMvcBuilders.standaloneSetup(this.courseController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("_id", is(course.getId()), Long.class))
                .andExpect(jsonPath("name", is(course.getName())))
                .andExpect(jsonPath("category", is(course.getCategory())));
    }

    /**
     * Method under test: {@link CourseController#update(Long, Course)}
     */
    @Test
    @DisplayName("Should throw an exception when updating an invalid course ID")
    void testUpdateNotFound() throws Exception {
        Course course = TestData.createValidCourse();
        when(this.courseService.update((Long) any(), (Course) any())).thenThrow(new RecordNotFoundException(1L));

        String content = (new ObjectMapper()).writeValueAsString(course);
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(API_ID, 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
        assertThrows(NestedServletException.class, () -> {
            ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(this.courseController)
                    .build()
                    .perform(requestBuilder);
            actualPerformResult.andExpect(status().isNotFound());
        });
    }

    /**
     * Method under test: {@link CourseController#update(Long, Course)}
     */
    @Test
    @DisplayName("Should throw exception when id is not valid - update")
    void testUpdateInvalid() throws Exception {
        Course course = TestData.createValidCourse();
        String content = (new ObjectMapper()).writeValueAsString(course);

        // invalid id and valid course
        assertThrows(NestedServletException.class, () -> {
            MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(API_ID, -1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
            ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(this.courseController)
                    .build()
                    .perform(requestBuilder);
            actualPerformResult.andExpect(status().isBadRequest());
        });
        assertThrows(IllegalArgumentException.class, () -> {
            MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(API_ID, null)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
            ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(this.courseController)
                    .build()
                    .perform(requestBuilder);
            actualPerformResult.andExpect(status().isBadRequest());
        });
    }
}
