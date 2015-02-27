package com.tcts.controller;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.tcts.common.SessionData;
import com.tcts.database.DatabaseFacade;
import com.tcts.datamodel.Teacher;
import com.tcts.formdata.CreateEventFormData;
import com.tcts.formdata.Errors;

/**
 * A controller for the screens used to create a new event (a class for volunteers to help with).
 */
@Controller
public class CreateEventController {

    @Autowired
    private DatabaseFacade database;


    @InitBinder
    private void initBinder(WebDataBinder binder) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        CustomDateEditor dateEditor = new CustomDateEditor(dateFormat, true);
        binder.registerCustomEditor(Date.class, dateEditor);
    }

    @RequestMapping(value="/createEvent", method= RequestMethod.GET)
    public String showCreateEventPage(HttpSession session, Model model) throws SQLException {
        SessionData sessionData = SessionData.fromSession(session);
        if (sessionData.getTeacher() == null) {
            throw new RuntimeException("Cannot navigate to this page unless you are a logged-in teacher.");
        }
        model.addAttribute("formData", new CreateEventFormData());
        return showFormWithErrors(model, null);
    }

    /**
     * A subroutine used to set up and then show the register teacher form. It
     * returns the string, so you can invoke it as "return showFormWithErrorMessage(...)".
     */
    private String showFormWithErrors(Model model, Errors errors) throws SQLException {
        model.addAttribute("allowedDates", database.getAllowedDates());
        model.addAttribute("allowedTimes", database.getAllowedTimes());
        model.addAttribute("errors", errors);
        return "createEvent";
    }


    @RequestMapping(value="/createEvent", method=RequestMethod.POST)
    public String createEvent(
            HttpSession session,
            Model model,
            @ModelAttribute("formData") CreateEventFormData formData)
        throws SQLException
    {
        SessionData sessionData = SessionData.fromSession(session);
        Teacher teacher = sessionData.getTeacher();
        if (teacher == null) {
            throw new RuntimeException("Cannot navigate to this page unless you are a logged-in teacher.");
        }

        // --- Validation Rules ---
        Errors errors = formData.validate();
        if (errors.hasErrors()) {
            return showFormWithErrors(model, errors);
        }

        // --- Create Event ---
        database.insertEvent(teacher.getUserId(), formData);

        // --- Navigate onward ---
        return "redirect:" + sessionData.getUser().getUserType().getHomepage();
    }
    
}
