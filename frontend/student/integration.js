//const API_BASE_URL = "http://localhost:8080"; //whatever the API base url turns out to be

//for index.html (Login)
async function handleLoginSubmit(event) {
    event.preventDefault(); // stops the page from refreshing
    
    // grab the actual data from the input fields
    const loginData = {
        studentId: document.getElementById('student-id').value,
        password: document.getElementById('password').value
    };

    const response = await fetch('LOGIN_URL', { // backend server that contains all login information
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData) 
    });
    
    const data = await response.text();
    console.log("Java replied: " + data);
}


/* For dashboard.html (Selecting a Test)
async function startTest(testId) { 
    const testRequest = { id: testId };

    const response = await fetch('START_URL', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(testRequest) 
    });

    const data = await response.text(); // get the text answer from Java
    console.log("Java replied: " + data);
}
*/

// For results.html (Viewing Scores) *JUST DRAFT*
async function showFinalScore() { 
    const resultId = sessionStorage.getItem('lastResultId'); 

    // getch results from api with the id entereed
    const response = await fetch('API_URL'); // backend url that stores all student test info or session info
    
    const scoreData = await response.json(); 
    console.log("Student Score is: " + scoreData.score);

}

// next button integration
async function getNextQuestion(ID, level) { // currently variables set to t-1 and 1, specifics see line 542 of test.html
    
    console.log("Next button clicked!"); //make sure button click is connected to this method with console log

    
    const testData = { 
        sessionId: ID, // be changed later when session storing is done
        difficulty: level // be changed later when difficulty leveling is done
    };
    

    // Now send it to NextQuestionHandler.java
    const response = await fetch('link', { // hiiii West, the link is the backend server for API logic and AI questions 
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(testData)
    });

    const data = await response.json(); //wait for responses from java
    console.log("AI Question Received:", data.text); // code currently doesn't change the interface, just logs results in console to make sure it works
}