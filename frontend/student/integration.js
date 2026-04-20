const API_BASE_URL = "http://localhost:8080";

//for index.html (Login)
async function handleLoginSubmit(event) {
    event.preventDefault(); // stops the page from refreshing
    
    // grab the actual data from the input fields
    const loginData = {
        studentId: document.getElementById('student-id').value,
        password: document.getElementById('password').value
    };

    const response = await fetch('LOGIN_URL', {
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

// For results.html (Viewing Scores)
async function showFinalScore() { 
    const resultId = sessionStorage.getItem('lastResultId'); 

    // getch results from api with the id entereed
    const response = await fetch('API_URL');
    
    const scoreData = await response.json(); // use .json() to get the object back
    console.log("Student Score is: " + scoreData.score);

}

// next button
async function getNextQuestion(difficulty) { 
    console.log("Next button clicked!");

    /*
    const testData = {
        sessionId: "TEST-SESSION-123", // be changed later when session storing is done
        difficulty: 1                  // be changed later when difficulty leveling is done
    };
    */

    // Now send it to NextQuestionHandler.java
    const response = await fetch('link', { // the link is the backend server for API logic and AI questions
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(difficulty)
    });

    const data = await response.json();
    console.log("AI Question Received:", data.text);
}