package org.eurekaclinical.i2b2.integration.webapp.servlet;

import java.io.IOException;
import javax.inject.Inject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.inject.Singleton;


@Singleton
public class LoginServlet extends HttpServlet {

    @Inject
    public LoginServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
    }
}
