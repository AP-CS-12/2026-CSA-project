import java.awt.event.ActionEvent;

public class ExamClass {
	
	//Variables
	private int currentQuestion;
	private int totalQuestions;
	private int difficulty;
	private int score;
	
	private boolean correct = false;
	

	
	//Click scanner
	public void actionPerformed(ActionEvent click) {
		
		if(currentQuestion < totalQuestions) {
			
			currentQuestion++;
			nextQuestion(correct);
		}
		else {
			submitExam();
		}
		
	}
	
	//Exam methods
	public void beginExam(int totalQ) {
		
		currentQuestion = 1;
		totalQuestions = totalQ; //total questions, given from teacher interface input
		
		// make the window full screen with no way to exit
		
	}
	
	public void submitExam() {
		//end the exam by closing the window
	}
	
	public void nextQuestion(boolean isCorrect) {
		
		if(isCorrect == true) {
			correct = true;
		}
		else {
			correct = false;
		}
		
		if(currentQuestion == totalQuestions) {
			//change button to say "Submit Exam"

		}
		
		else if(correct == false && difficulty >= 1) {
			//display easier question
		}
		else if(correct == true && difficulty <= 5) {
			//display harder question
		}
		else {
			//display a same difficulty question
		}
	}
	
	//Getters
	public int getCurrentQ() {
		return currentQuestion;
	}
	public int gettotalQ() {
		return totalQuestions;
	}
	public int getDifficulty() {
		return difficulty;
	}
	public int getScore() {
		return score;
	}
	public int getPercent() {
		return (int)((double)score/totalQuestions*100);
	}
}
