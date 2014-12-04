<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<html>
<head>
    <title>Teach Children To Save - Add Volunteer</title>
</head>
<body>
<%@include file="include/header.jsp" %>
<h2>Volunteer Information</h2>
<form:form method="POST" action="addVolunteer.htm" modelAttribute="volunteer">
    <div id="container" class="sansserif">
    	<div class="row_div">
	    	<div class="row_div_left">
		        <div class="caption_div"><form:label path="firstName">First Name</form:label></div>
		        <div class="field_div row_div_field_left_joint"><form:input path="firstName" /></div>
		     </div>
	        <div class="row_div_right">
	        	<div class="row_div_right"><form:label path="lastName">Last Name</form:label></div>
	        	<div class="field_div"><form:input path="lastName" /></div>
	        </div>
        </div>
    
    <div class="row_div">
    	<div class="row_div_left">
	        <div class="caption_div"><form:label path="emailAddress">Email Address</form:label></div>
	        <div class="field_div row_div_field_left_joint"><form:input path="emailAddress" /></div>
		</div>        
    
	    <div class="row_div_right">
	        <div class="caption_div"><form:label path="confirmEmailAddress">Confirm Email Address</form:label></div>
	        <div class="field_div"><form:input path="confirmEmailAddress" /></div>
	    </div>
    </div>
    
    <div class="row_div">
    	<div class="row_div_left">
            <div class="caption_div"><form:label path="password">password</form:label></div>
	        <div class="field_div row_div_field_left_joint"><form:input type="password" path="password" /></div>
    	</div>
    
    	<div class="row_div_right">
	        <div class="caption_div"><form:label path="confirmPassword">Re-Enter Password</form:label></div>
	        <div class="field_div"><form:input type="password" path="confirmPassword" /></div>
    	</div>
    </div>
    
    <div class="row_div">
    	<div class="row_div_left">
	        <div class="caption_div"><form:label path="addressLine1">Work Address Line1</form:label></div>
	        <div class="field_div row_div_field_left_joint"><form:input path="addressLine1" /></div>
        </div>
       
        <div class="row_div_right">
	        <div class="caption_div"><form:label path="addressLine2">Work Address Line2</form:label></div>
	        <div class="field_div"><form:input path="addressLine2" /></div>
    	</div>
    </div>
    
    <div class="row_div">
	    <div class="row_div_left">
	        <div class="caption_div"><form:label path="city">City</form:label></div>
	        <div class="field_div row_div_field_left_joint"><form:input path="city" /></div>
	    </div>
    
	    <div class="row_div_right">
	        <div class="caption_div"><form:label path="state">State</form:label></div>
	        <div class="field_div"><form:input path="state" /></div>
	    </div>
    </div>
    
    <div class="row_div">
    	<div class="row_div_left">
	        <div><form:label path="zipcode">zip</form:label></div>
	        <div><form:input path="zipcode" /></div>
        </div>
    </div>
    
    <div class="row_div">
        <div>
            <input type="submit" value="Submit"/>
        </div>
    </div>
  </div>
</form:form>
</body>
</html>