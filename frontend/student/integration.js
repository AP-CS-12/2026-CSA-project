const API_BASE_URL = "whatever it is";

// For index.html (Login)
async function handleLoginSubmit(event) {
    event.preventDefault(); // Stops the page from refreshing
    
    // Grab the actual data from the input fields
    const loginData = {
        studentId: document.getElementById('student-id').value,
        password: document.getElementById('password').value
    };

    const response = await fetch('LOGIN_URL', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData) // Now Java gets a nice, clean object
    });
    
    const data = await response.text();
    console.log("Java replied: " + data);
}


// For dashboard.html (Selecting a Test)
async function startTest(testId) { 
    const testRequest = { id: testId };

    const response = await fetch('START_URL', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(testRequest) 
    });

    const data = await response.text(); // Get the text answer from Java
    console.log("Java replied: " + data);
}


// For results.html (Viewing Scores)
async function showFinalScore() { 
    const resultId = sessionStorage.getItem('lastResultId'); 

    // getch results from api with the id entereed
    const response = await fetch('API_URL');
    
    const scoreData = await response.json(); // Use .json() to get the object back
    console.log("Student Score is: " + scoreData.score);

}