<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <title>Teach Children To Save - Add School</title>
        <%@include file="include/commonHead.jsp"%>
    </head>
    <body class="">

        <a href="#main" class="ada-read">Skip to main content</a>

        <div class="decor"></div>

        <%@include file="include/header.jsp" %>

        <div class="mainCnt">

            <%@include file="include/navigation.jsp" %>

            <main id="main">

                <h1>Add New School</h1>

                <c:if test="${not empty errorMessage}">
                    <div class="errorMessage">
                        <c:out value="${errorMessage}" default=""  />
                    </div>
                </c:if>

                <div>

                    <form:form method="POST" action="addSchool.htm" modelAttribute="formData">
                    

                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">School Name</div>
                                    <form:input path="schoolName"/>
                                </div>
                            </label>
                        </div>

                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School Address Line1
                                    </div>
                                    <form:input path="schoolAddress1" />
                                </div>
                            </label>
                        </div>

                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        Schoo Address Line2
                                    </div>
                                    <form:input path="schoolAddress2" />
                                </div>
                            </label>
                        </div>

                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School City
                                    </div>
                                    <form:input path="city" />
                                </div>
                            </label>
                        </div>
                        
                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School Zip
                                    </div>
                                    <form:input path="zip" />
                                </div>
                            </label>
                        </div>
                        
                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School County
                                    </div>
                                    <form:input path="county" />
                                </div>
                            </label>
                        </div>
                        
                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School District
                                    </div>
                                    <form:input path="district" />
                                </div>
                            </label>
                        </div>
                        
                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School State
                                    </div>
                                    <form:input path="state" />
                                </div>
                            </label>
                        </div>
                        
                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        School Phone
                                    </div>
                                    <form:input path="phone" />
                                </div>
                            </label>
                        </div>

                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        LMI Eligible
                                    </div>
                                    <form:input path="lmiEligible" />
                                </div>
                            </label>
                        </div>

                        <div class="formElementCnt">
                            <label>
                                <div class="inputCnt">
                                    <div class="info">
                                        SLC
                                    </div>
                                    <form:input path="SLC"/>
                                </div>
                            </label>
                        </div>

                        <button type="submit" value="Create">Create</button>
                        <button type="button" onclick="js.loadURL('schools.htm')">Cancel</button>

                    </form:form>

                </div>
                <div>
                    <button onclick="js.loadURL('viewEditSchools.htm')" class="editOrRegister cancel">Cancel</button>
                </div>

            </main>

        </div><%-- mainCnt --%>

        <%@include file="include/footer.jsp"%>
    </body>
</html>
