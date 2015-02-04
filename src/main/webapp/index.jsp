<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en-US">
    <head>

        <title>Teach Children To Save</title>
        <%@include file="WEB-INF/pages/include/commonHead.jsp"%>

    </head>
    <body class="home">

        <a href="#main" class="ada-read">Skip to main content</a>

        <%@include file="WEB-INF/pages/include/header.jsp" %>

        <div class="mainCnt">

        <%@include file="WEB-INF/pages/include/navigation.jsp" %>

            <main id="main">
                <h1>
                    2015 Teach Children to Save Day!
                    <br />
                    April 21, 22, and 23
                </h1>

                <h2>
                    3<sup>rd</sup> and 4<sup>th</sup> grade students learn critical lessons on personal finance and economics in a 45 minute lesson
                </h2>

                <h3>Teachers</h3>

                <p>Why participate?</p>

                <ul class="program_highlights">
                    <li><span>Most students don't get these financial literacy lessons any other place</span></li>
                    <li><span>Research has shown that when people are taught the basics of money management as children they are more likely to be fiscally fit as adults</span></li>
                    <li><span>It only takes 45 minutes</span></li>
                </ul>

                <button onclick="js.loadURL('registerTeacher.htm');">Sign up my class</button>

                <h3>Volunteers</h3>

                <p>Why volunteer?</p>

                <ul class="program_highlights">
                    <li><span>Research has shown that when people are taught the basics of money management as children they are more likely to be fiscally fit as adults</span></li>
                    <li><span>Provide valuable financial lessons most children don't won't receive any other time</span></li>
                    <li><span>Choose from a variety of locations that are convenient for you</span></li>
                    <li><span>It only takes 45 minutes</span></li>
                    <li><span>You'll receive all the training material you need</span></li>
                </ul>

                <button onclick="js.loadURL('registerVolunteer.htm');">Volunteer</button>

                <h3>Already Registered?</h3>
                <button onclick="js.loadURL('login.htm');">Sign In</button>

            </main>

            <aside>
                <img src="tcts/img/logo-tcts.png" alt="" aria-hidden="true">
                <%--<img src="tcts/img/iStock_000019109215Small-happy-kids.jpg" alt="" aria-hidden="true">--%>
            </aside>

        </div><%-- .mainCnt --%>

        <%@include file="WEB-INF/pages/include/footer.jsp" %>
    </body>
</html>