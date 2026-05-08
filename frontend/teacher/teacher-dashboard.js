/* ============================================================
         DEMO DATA + STATE
         ------------------------------------------------------------
         This whole script is front-end demo logic.
         It uses localStorage so your demo remembers courses/tests.
         Later, these functions can be replaced with real API calls.
         ============================================================ */

const APP_CONFIG = {
  storageKey: "teacherDashboardDataV3",
  maxPdfSizeInMb: 8,
  acceptedCurriculumFileTypes: ["application/pdf"],
};

const STORAGE_KEY = APP_CONFIG.storageKey;

let selectedCourseId = null;
let selectedTestId = null;
let selectedResultId = null;
let draftQuestions = [];
let selectedCourseFile = null;
let appHistory = [];
let appHistoryIndex = -1;
let isRestoringNavigation = false;

/* Placeholder question bank separated by type. */
const questionBank = [
  {
    id: "MC-001",
    type: "Multiple Choice",
    topic: "Sorting",
    difficulty: "Medium",
    question: "What is the average-case time complexity of merge sort?",
    choices: {
      A: "O(n)",
      B: "O(n log n)",
      C: "O(n²)",
      D: "O(log n)",
    },
    correct: "B",
  },
  {
    id: "MC-002",
    type: "Multiple Choice",
    topic: "Arrays",
    difficulty: "Easy",
    question: "Which index is the first element of an array stored at in Java?",
    choices: {
      A: "0",
      B: "1",
      C: "-1",
      D: "array.length",
    },
    correct: "A",
  },
  {
    id: "FRQ-001",
    type: "Free Response",
    topic: "ArrayLists",
    difficulty: "Medium",
    question:
      "Write a method that removes all even integers from an ArrayList<Integer>.",
    correct: "Teacher rubric placeholder",
  },
  {
    id: "FRQ-002",
    type: "Free Response",
    topic: "OOP",
    difficulty: "Hard",
    question:
      "Design a class hierarchy for different types of assessments in a testing platform.",
    correct: "Teacher rubric placeholder",
  },
];

/* ============================================================
   PLACEHOLDER RESULT DATA
   ------------------------------------------------------------
   Replace this array with backend/API assessment submissions later.
   Each result belongs to one student + one test.
   The UI below is written so this can be swapped out cleanly.
   ============================================================ */
const completedTests = [
  {
    id: "RES-001",
    studentId: "STU-001",
    student: "Jane Student",
    testId: "TEST-001",
    test: "Sorting Algorithms Quiz",
    courseId: "COURSE-001",
    rawScore: 19,
    totalPoints: 20,
    date: "Apr 18, 2026",
    status: "Completed",
    questionResults: [
      {
        questionId: "MC-001",
        topic: "Sorting",
        skill: "Merge sort time complexity",
        pointsEarned: 1,
        totalPoints: 1,
      },
      {
        questionId: "FRQ-001",
        topic: "ArrayLists",
        skill: "ArrayList mutation",
        pointsEarned: 9,
        totalPoints: 9,
      },
      {
        questionId: "TRACE-001",
        topic: "Tracing",
        skill: "Loop tracing",
        pointsEarned: 9,
        totalPoints: 10,
      },
    ],
  },
  {
    id: "RES-002",
    studentId: "STU-002",
    student: "Maya Patel",
    testId: "TEST-001",
    test: "Sorting Algorithms Quiz",
    courseId: "COURSE-001",
    rawScore: 16,
    totalPoints: 20,
    date: "Apr 19, 2026",
    status: "Completed",
    questionResults: [
      {
        questionId: "MC-001",
        topic: "Sorting",
        skill: "Merge sort time complexity",
        pointsEarned: 1,
        totalPoints: 1,
      },
      {
        questionId: "FRQ-001",
        topic: "ArrayLists",
        skill: "ArrayList mutation",
        pointsEarned: 6,
        totalPoints: 9,
      },
      {
        questionId: "TRACE-001",
        topic: "Tracing",
        skill: "Loop tracing",
        pointsEarned: 9,
        totalPoints: 10,
      },
    ],
  },
  {
    id: "RES-003",
    studentId: "STU-003",
    student: "Alex Chen",
    testId: "TEST-002",
    test: "Arrays and ArrayLists",
    courseId: "COURSE-002",
    rawScore: 12,
    totalPoints: 20,
    date: "Apr 17, 2026",
    status: "Completed",
    questionResults: [
      {
        questionId: "MC-002",
        topic: "Arrays",
        skill: "Array indexing",
        pointsEarned: 1,
        totalPoints: 1,
      },
      {
        questionId: "TRACE-002",
        topic: "Tracing",
        skill: "Array traversal",
        pointsEarned: 5,
        totalPoints: 9,
      },
      {
        questionId: "FRQ-002",
        topic: "OOP",
        skill: "Class design",
        pointsEarned: 6,
        totalPoints: 10,
      },
    ],
  },
];

const UNDERSTANDING_LEVELS = [
  {
    name: "Emerging",
    min: 0,
    description: "Needs direct support before independent AP-style work.",
  },
  {
    name: "Developing",
    min: 60,
    description: "Understands basics but still has gaps in application.",
  },
  {
    name: "Proficient",
    min: 75,
    description: "Meets AP-level expectations on most skills.",
  },
  {
    name: "Extending",
    min: 88,
    description: "Strong AP-level performance with consistent reasoning.",
  },
  {
    name: "Beyond AP",
    min: 95,
    description: "Exceeds AP-level expectations with advanced mastery.",
  },
];

let teacherData = normalizeTeacherData(loadData());
saveData();

function loadData() {
  const saved = localStorage.getItem(STORAGE_KEY);

  if (saved) {
    return JSON.parse(saved);
  }

  return {
    teacher: null,

    courses: [
      {
        id: "COURSE-001",
        name: "AP CSA Block B",
        code: "CSA-B-2026",
        unit: "Unit 7 — ArrayLists",
        pacing: "On Track",
        description:
          "Students are working on ArrayLists, traversal patterns, and common AP-style algorithms.",
        curriculumAttachment: null,
      },
      {
        id: "COURSE-002",
        name: "AP CSA Block A",
        code: "CSA-A-2026",
        unit: "Unit 6 — Arrays",
        pacing: "Needs Review",
        description:
          "Students are reviewing arrays, indexing errors, and basic sorting algorithm traces.",
        curriculumAttachment: null,
      },
    ],

    students: [
      {
        id: "STU-001",
        name: "Jane Student",
        email: "jane@example.com",
        courseId: "COURSE-001",
        status: "Confirmed",
      },
      {
        id: "STU-002",
        name: "Maya Patel",
        email: "maya@example.com",
        courseId: "COURSE-001",
        status: "Confirmed",
      },
      {
        id: "STU-003",
        name: "Alex Chen",
        email: "alex@example.com",
        courseId: "COURSE-002",
        status: "Confirmed",
      },
    ],

    joinRequests: [
      {
        id: "REQ-001",
        name: "Jordan Lee",
        courseId: "COURSE-001",
      },
    ],

    tests: [
      {
        id: "TEST-001",
        title: "Sorting Algorithms Quiz",
        courseId: "COURSE-001",
        type: "Test",
        timeLimit: "30",
        difficulty: "Medium",
        description:
          "Covers selection sort, insertion sort, merge sort, and algorithm tracing.",
        questions: [questionBank[0], questionBank[2]],
      },
      {
        id: "TEST-002",
        title: "Arrays and ArrayLists",
        courseId: "COURSE-002",
        type: "Assignment",
        timeLimit: "25",
        difficulty: "Easy",
        description:
          "Checks Java array indexing, traversal, and ArrayList methods.",
        questions: [questionBank[1]],
      },
    ],
  };
}

function normalizeTeacherData(data) {
  // Keeps older localStorage demo data from crashing after new fields are added.
  return {
    teacher: data.teacher || null,
    courses: (data.courses || []).map((course) => ({
      ...course,
      curriculumAttachment: course.curriculumAttachment || null,
    })),
    students: data.students || [],
    joinRequests: data.joinRequests || [],
    tests: (data.tests || []).map((test) => ({
      ...test,
      questions: test.questions || [],
    })),
  };
}

function saveData() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(teacherData));
}

/* ============================================================
         AUTH / ACCESS CONTROL
         ------------------------------------------------------------
         The dashboard wrapper is hidden until teacherData.teacher exists.
         This prevents teachers from seeing creation/results tools before
         signing in.
         ============================================================ */

function signInWithGoogle() {
  teacherData.teacher = {
    name: "Demo Teacher",
    email: "teacher@example.com",
  };

  saveData();
  renderApp();
  resetNavigationHistory();
  showDashboard();
}

function signOutTeacher() {
  teacherData.teacher = null;
  selectedCourseId = null;
  selectedTestId = null;
  selectedResultId = null;
  draftQuestions = [];
  selectedCourseFile = null;

  saveData();
  renderApp();
  resetNavigationHistory();
}

function renderApp() {
  const isSignedIn = Boolean(teacherData.teacher);

  document.getElementById("login-screen").hidden = isSignedIn;
  document.getElementById("dashboard-screen").hidden = !isSignedIn;
  document.getElementById("teacher-badge").hidden = !isSignedIn;
  document.getElementById("teacher-nav").hidden = !isSignedIn;

  if (!isSignedIn) {
    return;
  }

  document.getElementById("teacher-name").textContent =
    teacherData.teacher.name;
  document.getElementById("teacher-avatar").textContent = getInitials(
    teacherData.teacher.name,
  );

  renderDashboard();
  renderSelectedCourse();
  renderDraftQuestions();
  renderQuestionBank();
  setupCourseCurriculumUpload();
}

function getInitials(name) {
  return name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

/* ============================================================
         SCREEN NAVIGATION
         ============================================================ */

/* ============================================================
         SCREEN NAVIGATION
         ============================================================ */

function getActiveCourseTab() {
  const activeTab = document.querySelector(".course-tab.active");
  return activeTab ? activeTab.dataset.courseTab : "overview";
}

function getCurrentNavigationState() {
  const activeScreen = document.querySelector(".app-screen.active-screen");

  return {
    screenName: activeScreen
      ? activeScreen.id.replace("screen-", "")
      : "dashboard",
    courseId: selectedCourseId,
    testId: selectedTestId,
    resultId: selectedResultId,
    courseTab: getActiveCourseTab(),
  };
}

function resetNavigationHistory() {
  appHistory = [];
  appHistoryIndex = -1;
  updateNavigationButtons();
}

function recordNavigationState() {
  if (isRestoringNavigation || !teacherData.teacher) {
    updateNavigationButtons();
    return;
  }

  const state = getCurrentNavigationState();
  const currentState = appHistory[appHistoryIndex];

  if (currentState && JSON.stringify(currentState) === JSON.stringify(state)) {
    updateNavigationButtons();
    return;
  }

  appHistory = appHistory.slice(0, appHistoryIndex + 1);
  appHistory.push(state);
  appHistoryIndex = appHistory.length - 1;
  updateNavigationButtons();
}

function updateNavigationButtons() {
  const backButton = document.getElementById("app-back-button");

  if (!backButton) {
    return;
  }

  backButton.disabled = appHistoryIndex <= 0;
}

function navigateBack() {
  if (appHistoryIndex <= 0) {
    return;
  }

  appHistoryIndex -= 1;
  restoreNavigationState(appHistory[appHistoryIndex]);
}

function restoreNavigationState(state) {
  if (!state) {
    return;
  }

  isRestoringNavigation = true;
  selectedCourseId = state.courseId;
  selectedTestId = state.testId;
  selectedResultId = state.resultId || null;

  if (state.screenName === "test-detail" && state.testId) {
    const test = teacherData.tests.find((test) => test.id === state.testId);
    if (test) {
      renderTestDetail(test);
    }
  }

  if (state.screenName === "student-result-detail" && state.resultId) {
    const result = completedTests.find((entry) => entry.id === state.resultId);
    if (result) {
      renderStudentResultDetail(result);
    }
  }

  showScreen(state.screenName);

  if (state.screenName === "course-detail") {
    showCourseTab(state.courseTab || "overview");
  }

  renderSelectedCourse();
  isRestoringNavigation = false;
  updateNavigationButtons();
}

function showScreen(screenName) {
  document.querySelectorAll(".app-screen").forEach((screen) => {
    screen.classList.remove("active-screen");
  });

  document
    .getElementById(`screen-${screenName}`)
    .classList.add("active-screen");

  document.querySelectorAll("[data-screen-button]").forEach((button) => {
    button.classList.remove("active");
  });

  const activeButton = document.querySelector(
    `[data-screen-button="${screenName}"]`,
  );

  if (activeButton) {
    activeButton.classList.add("active");
  }

  window.scrollTo({ top: 0, behavior: "smooth" });
}

function showDashboard() {
  selectedCourseId = null;
  selectedTestId = null;
  selectedResultId = null;
  showScreen("dashboard");
  renderDashboard();
  recordNavigationState();
}

function openCourse(courseId) {
  selectedCourseId = courseId;
  selectedTestId = null;
  selectedResultId = null;
  showScreen("course-detail");
  showCourseTab("overview");
  renderSelectedCourse();
  recordNavigationState();
}

function returnToCurrentCourse() {
  showScreen("course-detail");
  showCourseTab("tests");
  recordNavigationState();
}

function showCourseTabFromTest(tabName) {
  showScreen("course-detail");
  showCourseTab(tabName);
  recordNavigationState();
}

function showResultsForCurrentTest() {
  const assessmentId = selectedTestId;

  showScreen("course-detail");
  showCourseTab("results");

  if (assessmentId) {
    viewAssessmentResults(assessmentId, false);
  }

  recordNavigationState();
}

function returnToSelectedAssessmentResults() {
  showScreen("course-detail");
  showCourseTab("results");

  if (selectedTestId) {
    viewAssessmentResults(selectedTestId, false);
  }

  recordNavigationState();
}

function showTestDetail(testId) {
  selectedTestId = testId;
  selectedResultId = null;

  const test = teacherData.tests.find((test) => test.id === testId);

  if (!test) {
    alert("That test could not be found.");
    return;
  }

  renderTestDetail(test);
  showScreen("test-detail");
  recordNavigationState();
}

function showCourseTab(tabName) {
  document.querySelectorAll(".course-tab-screen").forEach((screen) => {
    screen.classList.remove("active-course-tab");
  });

  document.querySelectorAll(".course-tab").forEach((tab) => {
    tab.classList.remove("active");
  });

  document
    .getElementById(`course-tab-${tabName}`)
    .classList.add("active-course-tab");

  const activeTab = document.querySelector(`[data-course-tab="${tabName}"]`);

  if (activeTab) {
    activeTab.classList.add("active");
  }

  renderSelectedCourse();
  recordNavigationState();
}
/* ============================================================
         DASHBOARD RENDERING
         ============================================================ */

function renderDashboard() {
  const courseCount = teacherData.courses.length;
  const studentCount = teacherData.students.length;
  const testCount = teacherData.tests.length;

  document.getElementById("course-count").textContent = courseCount;
  document.getElementById("student-count").textContent = studentCount;
  document.getElementById("test-count").textContent = testCount;
  document.getElementById("completed-count").textContent =
    completedTests.length;

  const courseList = document.getElementById("dashboard-course-list");

  if (teacherData.courses.length === 0) {
    courseList.innerHTML = `
            <div class="empty-state">
              <h3>No courses yet</h3>
              <p>Create your first course to begin adding students and tests.</p>
            </div>
          `;
    return;
  }

  courseList.innerHTML = teacherData.courses
    .map((course) => {
      const students = getStudentsForCourse(course.id);
      const tests = getTestsForCourse(course.id);
      const results = getResultsForCourse(course.id);

      return `
            <article class="course-card clickable-card" onclick="openCourse('${course.id}')">
              <div class="course-card-top">
                <span class="badge badge-navy">${course.code}</span>
                <span class="badge badge-slate">${course.pacing}</span>
              </div>

              <h3>${course.name}</h3>
              <p>${course.description}</p>

              <div class="unit-pill">
                Current Unit: ${escapeHTML(course.unit)}
              </div>

              <div class="curriculum-pill ${course.curriculumAttachment ? "has-file" : ""}">
                ${course.curriculumAttachment ? `Curriculum: ${escapeHTML(course.curriculumAttachment.name)}` : "No curriculum PDF yet"}
              </div>

              <div class="course-meta">
                <span>${students.length} student(s)</span>
                <span>${tests.length} test(s)</span>
                <span>${results.length} result(s)</span>
              </div>

              <button class="btn btn-sm btn-primary card-action" type="button">
                Open Course →
              </button>
            </article>
          `;
    })
    .join("");
}

function openCreateCoursePanel() {
  document.getElementById("create-course-panel").hidden = false;
  document
    .getElementById("create-course-panel")
    .scrollIntoView({ behavior: "smooth" });
}

function closeCreateCoursePanel() {
  document.getElementById("create-course-panel").hidden = true;
}

/* ============================================================
         COURSE RENDERING
         ============================================================ */

function renderSelectedCourse() {
  if (!selectedCourseId) {
    return;
  }

  const course = getSelectedCourse();
  if (!course) {
    return;
  }

  const students = getStudentsForCourse(course.id);
  const tests = getTestsForCourse(course.id);
  const results = getResultsForCourse(course.id);

  document.getElementById("course-detail-code").textContent = course.code;
  document.getElementById("course-detail-name").textContent = course.name;
  document.getElementById("course-detail-description").textContent =
    course.description;
  document.getElementById("course-detail-students").textContent =
    students.length;
  document.getElementById("course-detail-tests").textContent = tests.length;
  document.getElementById("course-detail-unit").textContent = course.unit;
  document.getElementById("course-join-code").textContent = course.code;

  renderCourseOverview(course, students, tests, results);
  renderCourseStudents(students);
  renderJoinRequests();
  renderCourseTests(tests);
  renderCourseResults(results);
}

function renderCourseOverview(course, students, tests, results) {
  const overview = document.getElementById("course-overview-content");

  overview.innerHTML = `
          <div class="preview-item">
            <strong>Current Unit</strong>
            <span>${course.unit}</span>
          </div>

          <div class="preview-item">
            <strong>Pacing</strong>
            <span>${course.pacing}</span>
          </div>

          <div class="preview-item">
            <strong>Class Size</strong>
            <span>${students.length} students enrolled</span>
          </div>

          <div class="preview-item">
            <strong>Assessments</strong>
            <span>${tests.length} created, ${results.length} completed results</span>
          </div>

          <div class="preview-item">
            <strong>Curriculum PDF</strong>
            <span>${course.curriculumAttachment ? `${course.curriculumAttachment.name} · ${formatBytes(course.curriculumAttachment.size)}` : "No curriculum file attached yet"}</span>
          </div>
        `;
}

function renderCourseStudents(students) {
  const studentList = document.getElementById("course-student-list");

  if (students.length === 0) {
    studentList.innerHTML = `
            <div class="empty-state">
              <h3>No students yet</h3>
              <p>Add students by email or approve join code requests.</p>
            </div>
          `;
    return;
  }

  studentList.innerHTML = students
    .map((student) => {
      return `
            <article class="student-card">
              <div>
                <h3>${student.name}</h3>
                <p>${student.email}</p>
              </div>

              <span class="badge badge-success">${student.status}</span>
            </article>
          `;
    })
    .join("");
}

function renderJoinRequests() {
  const requestList = document.getElementById("join-request-list");
  const requests = teacherData.joinRequests.filter(
    (request) => request.courseId === selectedCourseId,
  );

  if (requests.length === 0) {
    requestList.innerHTML = `
            <div class="empty-state compact">
              <p>No pending join requests.</p>
            </div>
          `;
    return;
  }

  requestList.innerHTML = requests
    .map((request) => {
      return `
            <div class="request-item">
              <div>
                <strong>${request.name}</strong>
                <p>Requested to join this course.</p>
              </div>

              <button class="btn btn-sm btn-primary" onclick="acceptJoinRequest('${request.id}')">
                Accept
              </button>
            </div>
          `;
    })
    .join("");
}

function renderCourseTests(tests) {
  const tableBody = document.getElementById("course-test-table-body");

  if (tests.length === 0) {
    tableBody.innerHTML = `
            <tr>
              <td colspan="7" class="empty-table">No tests or assignments created yet.</td>
            </tr>
          `;
    return;
  }

  tableBody.innerHTML = tests
    .map((test) => {
      return `
            <tr>
              <td>${test.title}</td>
              <td class="secondary">${test.type}</td>
              <td>${(test.questions || []).length}</td>
              <td class="secondary">${test.timeLimit} min</td>
              <td><span class="badge badge-slate">${test.difficulty}</span></td>
              <td><span class="badge badge-success">Created</span></td>
              <td>
                <button class="btn btn-sm btn-primary" onclick="showTestDetail('${test.id}')">
                  Open
                </button>
              </td>
            </tr>
          `;
    })
    .join("");
}

function renderTestDetail(test) {
  document.getElementById("test-detail-title").textContent = test.title;
  document.getElementById("test-detail-description").textContent =
    test.description || "No description provided for this test.";

  document.getElementById("test-detail-meta").innerHTML = `
    <div class="preview-item">
      <strong>Type</strong>
      <span>${test.type}</span>
    </div>
    <div class="preview-item">
      <strong>Time Limit</strong>
      <span>${test.timeLimit} minutes</span>
    </div>
    <div class="preview-item">
      <strong>Difficulty</strong>
      <span>${test.difficulty}</span>
    </div>
    <div class="preview-item">
      <strong>Question Count</strong>
      <span>${(test.questions || []).length} question(s)</span>
    </div>
  `;

  renderTestQuestionList(test.questions || []);
}

function renderTestQuestionList(questions) {
  const list = document.getElementById("test-detail-question-list");

  if (!list) {
    return;
  }

  if (questions.length === 0) {
    list.innerHTML = `
      <div class="empty-state compact">
        <p>No questions were added to this assessment.</p>
      </div>
    `;
    return;
  }

  list.innerHTML = questions
    .map((question, index) => {
      return `
        <div class="question-item">
          <div>
            <strong>${index + 1}. ${escapeHTML(question.question)}</strong>
            <p>${escapeHTML(question.type)} · ${escapeHTML(question.topic || "General")} · ${escapeHTML(question.difficulty || "Adaptive")}</p>
          </div>
        </div>
      `;
    })
    .join("");
}

function renderCourseResults(results) {
  const assessmentList = document.getElementById("assessment-results-list");

  if (!assessmentList) {
    return;
  }

  const tests = getTestsForCourse(selectedCourseId);

  // Keep the Results tab streamlined: show assessment cards first.
  // Test-specific details appear only after the teacher clicks View Results.
  selectedTestId = null;
  selectedResultId = null;

  renderAssessmentResultCards(tests, results);
  hideSelectedAssessmentDetails();

  if (tests.length === 0) {
    updateResultsSummary([]);
    renderUnderstandingLevelGrid([]);
    renderTopicUnderstanding([]);
    renderLevelDistribution([]);
    renderClassPerformanceTable([], [], "all");
    renderQuestionLevelAnalysis(null, []);
  }
}

function hideSelectedAssessmentDetails() {
  document.querySelectorAll(".assessment-detail-section").forEach((section) => {
    section.hidden = true;
  });

  updateResultsSummary([]);
  renderUnderstandingLevelGrid([]);
  renderTopicUnderstanding([]);
  renderLevelDistribution([]);
  renderClassPerformanceTable([], [], "all");
  renderQuestionLevelAnalysis(null, []);
  renderSelectedStudentReport(null);

  setTextContent("selected-assessment-type-badge", "Assessment");
  setTextContent("selected-assessment-difficulty-badge", "Difficulty");
  setTextContent("selected-assessment-title", "Assessment Results");
  setTextContent(
    "selected-assessment-summary",
    "Choose an assessment above to view student completion and scores.",
  );
  setTextContent("selected-assessment-meta", "");
  setTextContent("selected-assessment-ai-summary", "");
  setTextContent("selected-assessment-average-circle", "—");
  setTextContent("teacher-alert", "");
  setTextContent("ai-summary", "");

  const actions = document.getElementById("teacher-actions");
  if (actions) {
    actions.innerHTML = "";
  }

  const completedStudentTable = document.getElementById(
    "assessment-completed-student-list",
  );
  const missingStudentList = document.getElementById(
    "assessment-missing-student-list",
  );

  if (completedStudentTable) {
    completedStudentTable.innerHTML = "";
  }

  if (missingStudentList) {
    missingStudentList.innerHTML = "";
  }
}

function renderSelectedStudentReport(result) {
  // Older result-layout drafts referenced an inline selected-student panel.
  // The current streamlined flow uses the dedicated student result detail page,
  // so this compatibility function intentionally does nothing.
  return result;
}

function setTextContent(id, value) {
  const node = document.getElementById(id);

  if (node) {
    node.textContent = value;
  }
}

function renderAssessmentResultCards(tests, results) {
  const assessmentList = document.getElementById("assessment-results-list");

  if (tests.length === 0) {
    assessmentList.innerHTML = `
      <div class="empty-state">
        <h3>No assessments yet</h3>
        <p>Create a test or assignment before viewing results.</p>
      </div>
    `;
    return;
  }

  assessmentList.innerHTML = tests
    .map((test) => {
      const testResults = results.filter((result) => result.testId === test.id);
      const students = getStudentsForCourse(test.courseId);
      const completedCount = testResults.length;
      const missingCount = Math.max(students.length - completedCount, 0);
      const averageScore =
        completedCount === 0 ? "—" : `${getAveragePercent(testResults)}%`;
      const completionText = `${completedCount}/${students.length} completed`;

      return `
        <article
          class="assessment-result-card ${
            selectedTestId === test.id ? "active-assessment-card" : ""
          }"
        >
          <div class="assessment-result-top">
            <span class="badge badge-navy">${escapeHTML(test.type)}</span>
            <span class="badge badge-slate">${escapeHTML(test.difficulty)}</span>
          </div>

          <h3>${escapeHTML(test.title)}</h3>
          <p>${escapeHTML(test.description || "No description provided.")}</p>

          <div class="assessment-result-stats">
            <div>
              <strong>${averageScore}</strong>
              <span>Average</span>
            </div>

            <div>
              <strong>${completedCount}</strong>
              <span>Completed</span>
            </div>

            <div>
              <strong>${missingCount}</strong>
              <span>Missing</span>
            </div>
          </div>

          <div class="unit-pill">${completionText}</div>

          <button
            class="btn btn-sm btn-primary card-action"
            type="button"
            onclick="viewAssessmentResults('${test.id}')"
          >
            View Results →
          </button>
        </article>
      `;
    })
    .join("");
}

function viewAssessmentResults(testId, shouldRecordHistory = true) {
  selectedTestId = testId;
  selectedResultId = null;

  const test = teacherData.tests.find((assessment) => assessment.id === testId);

  if (!test) {
    return;
  }

  const allCourseResults = getResultsForCourse(selectedCourseId);
  const assessmentResults = allCourseResults.filter((result) => {
    return result.testId === testId;
  });

  const students = getStudentsForCourse(selectedCourseId);

  document.querySelectorAll(".assessment-detail-section").forEach((section) => {
    section.hidden = false;
  });

  renderSelectedAssessmentHero(test, students, assessmentResults);
  updateResultsSummary(assessmentResults);
  renderAssessmentStudentRows(test, students, assessmentResults);
  renderClassReview(test, students, assessmentResults);
  renderLevelDistribution(assessmentResults);
  renderTopicUnderstanding(assessmentResults);
  renderUnderstandingLevelGrid(assessmentResults);
  renderClassPerformanceTable(students, assessmentResults, "all");
  renderQuestionLevelAnalysis(test, assessmentResults);
  renderSelectedStudentReport(null);
  resetStudentFilterChips();
  renderAssessmentResultCards(
    getTestsForCourse(selectedCourseId),
    allCourseResults,
  );

  document
    .getElementById("selected-assessment-panel")
    .scrollIntoView({ behavior: "smooth", block: "start" });

  if (shouldRecordHistory) {
    recordNavigationState();
  }
}

function renderSelectedAssessmentHero(test, students, assessmentResults) {
  const completedCount = assessmentResults.length;
  const averageScore = getAveragePercent(assessmentResults);
  const completionText = `${completedCount} of ${students.length} students have completed this ${test.type.toLowerCase()}.`;
  const topicSummaries = getTopicSummaries(assessmentResults);
  const strongest = getStrongestTopic(topicSummaries);
  const weakest = getWeakestTopic(topicSummaries);

  setTextContent("selected-assessment-type-badge", test.type);
  setTextContent("selected-assessment-difficulty-badge", test.difficulty);
  setTextContent("selected-assessment-title", test.title);
  setTextContent("selected-assessment-summary", completionText);
  setTextContent(
    "selected-assessment-meta",
    `${test.timeLimit} min · ${(test.questions || []).length} question(s) · ${completedCount}/${students.length} completed`,
  );
  setTextContent(
    "selected-assessment-average-circle",
    completedCount === 0 ? "—" : `${averageScore}%`,
  );

  const summaryNode = document.getElementById("selected-assessment-ai-summary");

  if (!summaryNode) {
    return;
  }

  if (completedCount === 0 || topicSummaries.length === 0) {
    summaryNode.innerHTML = `
      <div>No submissions yet. Once students complete this assessment, this area will summarize class strengths and priority review topics.</div>
    `;
    return;
  }

  summaryNode.innerHTML = `
    <div>${escapeHTML(strongest.topic)} is currently the clearest class strength.</div>
    <div>${escapeHTML(weakest.topic)} is the best candidate for targeted reteaching.</div>
  `;
}

function renderClassReview(test, students, assessmentResults) {
  const topicSummaries = getTopicSummaries(assessmentResults);
  const completedCount = assessmentResults.length;
  const averageScore = getAveragePercent(assessmentResults);
  const highRiskCount = assessmentResults.filter((result) => {
    const percent = getResultPercent(result);
    return percent < 60;
  }).length;
  const nearLineCount = assessmentResults.filter((result) => {
    const percent = getResultPercent(result);
    return percent >= 60 && percent < 75;
  }).length;
  const strongest = getStrongestTopic(topicSummaries);
  const weakest = getWeakestTopic(topicSummaries);

  if (completedCount === 0) {
    setTextContent(
      "teacher-alert",
      "No completed submissions yet. The AI class review will populate after students submit results.",
    );
    setTextContent(
      "ai-summary",
      "Waiting for student submissions before generating a class-level summary.",
    );
    setTeacherActions([
      "Check back after students complete the assessment.",
      "Use the missing-students panel to monitor outstanding submissions.",
    ]);
    return;
  }

  const weakestTopic = weakest ? weakest.topic : "the lowest-scoring topic";
  const strongestTopic = strongest ? strongest.topic : "the strongest topic";

  setTextContent(
    "teacher-alert",
    `${highRiskCount} student(s) are below 60%. Targeted reteaching around ${weakestTopic} is recommended.`,
  );
  setTextContent(
    "ai-summary",
    `The class average is ${averageScore}%. Performance is strongest in ${strongestTopic} and weakest in ${weakestTopic}.`,
  );

  setTeacherActions([
    `Run a short small-group review on ${weakestTopic}.`,
    `${nearLineCount} student(s) are near the proficiency line; give them a focused follow-up practice set.`,
    `Use ${strongestTopic} as a confidence-builder before moving into harder mixed problems.`,
  ]);
}

function setTeacherActions(actions) {
  const actionList = document.getElementById("teacher-actions");

  if (!actionList) {
    return;
  }

  actionList.innerHTML = actions
    .map((action) => `<li>${escapeHTML(action)}</li>`)
    .join("");
}

function renderLevelDistribution(results) {
  const target = document.getElementById("level-distribution");

  if (!target) {
    return;
  }

  if (results.length === 0) {
    target.innerHTML = `
      <div class="empty-state compact">
        <p>No completed submissions yet.</p>
      </div>
    `;
    return;
  }

  const levelCounts = getUnderstandingLevelCounts(results);
  const buckets = [...UNDERSTANDING_LEVELS].reverse().map((level) => ({
    label: level.name,
    count: levelCounts[level.name] || 0,
    className: getLevelClass(level.name),
  }));
  const maxCount = Math.max(...buckets.map((bucket) => bucket.count), 1);

  target.innerHTML = buckets
    .map((bucket) => {
      const width = Math.max(
        (bucket.count / maxCount) * 100,
        bucket.count ? 8 : 0,
      );

      return `
        <div class="dist-row">
          <div class="dist-label">${escapeHTML(bucket.label)}</div>
          <div class="dist-bar-track">
            <div class="dist-bar-fill ${bucket.className}" style="width:${width}%"></div>
          </div>
          <div class="dist-count">${bucket.count}</div>
        </div>
      `;
    })
    .join("");
}

function resetStudentFilterChips() {
  const chips = document.querySelectorAll("#student-filters .chip");

  chips.forEach((chip, index) => {
    chip.classList.toggle("active", index === 0);
  });
}

function filterAssessmentStudents(filter, clickedButton) {
  if (!selectedTestId) {
    return;
  }

  const allCourseResults = getResultsForCourse(selectedCourseId);
  const assessmentResults = allCourseResults.filter((result) => {
    return result.testId === selectedTestId;
  });
  const students = getStudentsForCourse(selectedCourseId);

  document.querySelectorAll("#student-filters .chip").forEach((chip) => {
    chip.classList.remove("active");
  });

  if (clickedButton) {
    clickedButton.classList.add("active");
  }

  renderClassPerformanceTable(students, assessmentResults, filter);
}

function renderClassPerformanceTable(
  students,
  assessmentResults,
  filter = "all",
) {
  const body = document.getElementById("student-results-body");

  if (!body) {
    return;
  }

  if (!students.length || !assessmentResults.length) {
    body.innerHTML = `
      <tr>
        <td colspan="6" class="empty-table">No completed student results yet.</td>
      </tr>
    `;
    return;
  }

  const rows = assessmentResults
    .map((result) => {
      const student = students.find((entry) => entry.id === result.studentId);
      const percent = getResultPercent(result);
      const level = getUnderstandingLevel(percent);
      const focus = getWeakestTopic(getTopicSummaries([result]));
      const risk = getRiskLabel(percent);

      return {
        result,
        student,
        percent,
        level,
        focus: focus ? focus.topic : "Keep Studying",
        risk,
      };
    })
    .filter((entry) => {
      if (filter === "high") {
        return entry.percent >= 85;
      }

      if (filter === "mid") {
        return entry.percent >= 60 && entry.percent < 85;
      }

      if (filter === "risk") {
        return entry.percent < 60 || entry.risk === "High";
      }

      return true;
    })
    .sort((a, b) => b.percent - a.percent);

  if (rows.length === 0) {
    body.innerHTML = `
      <tr>
        <td colspan="6" class="empty-table">No students match this filter.</td>
      </tr>
    `;
    return;
  }

  body.innerHTML = rows
    .map((entry) => {
      const studentName = entry.student
        ? entry.student.name
        : getResultStudentName(entry.result);
      const studentEmail = entry.student
        ? entry.student.email
        : "Submitted result";

      return `
        <tr>
          <td>
            <div class="student-cell">
              <strong>${escapeHTML(studentName)}</strong>
              <span class="secondary mono">${escapeHTML(studentEmail)}</span>
            </div>
          </td>
          <td>
            <span class="badge badge-level ${getLevelClass(entry.level.name)}">
              ${entry.level.name}
            </span>
          </td>
          <td><strong>${entry.percent}%</strong></td>
          <td class="secondary">${escapeHTML(entry.focus)}</td>
          <td>
            <span class="badge ${getRiskBadgeClass(entry.risk)}">${entry.risk}</span>
          </td>
          <td>
            <button class="btn btn-sm btn-primary" onclick="viewStudentResultFromAssessment('${entry.result.id}')">
              View Report
            </button>
          </td>
        </tr>
      `;
    })
    .join("");
}

function renderQuestionLevelAnalysis(test, results) {
  const body = document.getElementById("item-analysis-body");

  if (!body) {
    return;
  }

  if (!test || results.length === 0) {
    body.innerHTML = `
      <tr>
        <td colspan="4" class="empty-table">No question-level data yet.</td>
      </tr>
    `;
    return;
  }

  const questionMap = new Map();

  results.forEach((result) => {
    getResultTopicEntries(result).forEach((entry, index) => {
      const questionId = entry.questionId || `Question ${index + 1}`;

      if (!questionMap.has(questionId)) {
        questionMap.set(questionId, {
          questionId,
          topic: entry.topic || "General",
          earned: 0,
          total: 0,
          samples: 0,
        });
      }

      const summary = questionMap.get(questionId);
      summary.earned += Number(entry.pointsEarned || 0);
      summary.total += Number(entry.totalPoints || 0);
      summary.samples += 1;
    });
  });

  const rows = [...questionMap.values()];

  if (rows.length === 0) {
    body.innerHTML = `
      <tr>
        <td colspan="4" class="empty-table">No question-level data yet.</td>
      </tr>
    `;
    return;
  }

  body.innerHTML = rows
    .map((item, index) => {
      const percent = getPercent(item.earned, item.total);

      return `
        <tr>
          <td><strong>${escapeHTML(item.questionId || `Q${index + 1}`)}</strong></td>
          <td class="secondary">${escapeHTML(item.topic)}</td>
          <td><strong>${percent}%</strong></td>
          <td class="secondary">${item.samples} submission(s)</td>
        </tr>
      `;
    })
    .join("");
}

function getStrongestTopic(topicSummaries) {
  if (!topicSummaries || topicSummaries.length === 0) {
    return null;
  }

  return [...topicSummaries].sort((a, b) => b.percent - a.percent)[0];
}

function getWeakestTopic(topicSummaries) {
  if (!topicSummaries || topicSummaries.length === 0) {
    return null;
  }

  return [...topicSummaries].sort((a, b) => a.percent - b.percent)[0];
}

function getRiskLabel(percent) {
  if (percent >= 75) {
    return "Low";
  }

  if (percent >= 60) {
    return "Medium";
  }

  return "High";
}

function getRiskBadgeClass(risk) {
  if (risk === "Low") {
    return "badge-success";
  }

  if (risk === "Medium") {
    return "badge-accent";
  }

  return "badge-danger";
}

function renderAssessmentStudentRows(test, students, assessmentResults) {
  const completedStudentTable = document.getElementById(
    "assessment-completed-student-list",
  );
  const missingStudentList = document.getElementById(
    "assessment-missing-student-list",
  );

  const completedStudents = students
    .map((student) => {
      const result = assessmentResults.find((entry) => {
        return entry.studentId === student.id;
      });

      return {
        student,
        result,
      };
    })
    .filter((entry) => Boolean(entry.result));

  const missingStudents = students.filter((student) => {
    return !assessmentResults.some((entry) => entry.studentId === student.id);
  });

  if (completedStudents.length === 0) {
    completedStudentTable.innerHTML = `
      <tr>
        <td colspan="5" class="empty-table">
          No students have completed this assessment yet.
        </td>
      </tr>
    `;
  } else {
    completedStudentTable.innerHTML = completedStudents
      .map(({ student, result }) => {
        const percent = getResultPercent(result);
        const level = getUnderstandingLevel(percent);

        return `
          <tr>
            <td>
              <strong>${escapeHTML(student.name)}</strong>
              <div class="muted">${escapeHTML(student.email)}</div>
            </td>
            <td><strong>${percent}%</strong></td>
            <td>
              <span class="badge badge-level ${getLevelClass(level.name)}">
                ${level.name}
              </span>
            </td>
            <td class="secondary">${escapeHTML(result.date)}</td>
            <td>
              <button
                class="btn btn-sm btn-primary"
                onclick="viewStudentResultFromAssessment('${result.id}')"
              >
                View Report
              </button>
            </td>
          </tr>
        `;
      })
      .join("");
  }

  if (missingStudents.length === 0) {
    missingStudentList.innerHTML = `
      <div class="empty-state compact">
        <p>Everyone has completed this assessment.</p>
      </div>
    `;
    return;
  }

  missingStudentList.innerHTML = missingStudents
    .map((student) => {
      return `
        <article class="missing-student-item">
          <div>
            <strong>${escapeHTML(student.name)}</strong>
            <p>${escapeHTML(student.email)}</p>
          </div>
          <span class="badge badge-slate">Pending</span>
        </article>
      `;
    })
    .join("");
}

function viewStudentResultFromAssessment(resultId) {
  const result = completedTests.find((entry) => entry.id === resultId);

  if (!result) {
    return;
  }

  selectedResultId = resultId;
  selectedTestId = result.testId;
  selectedCourseId = result.courseId;

  renderStudentResultDetail(result);
  showScreen("student-result-detail");
  recordNavigationState();
}

function renderStudentResultDetail(result) {
  if (!result) {
    return;
  }

  const percent = getResultPercent(result);
  const level = getUnderstandingLevel(percent);
  const course = teacherData.courses.find(
    (entry) => entry.id === result.courseId,
  );
  const skillList = document.getElementById("student-result-skill-list");
  const recommendations = document.getElementById(
    "student-result-recommendations",
  );
  const assessmentReference = document.getElementById(
    "student-result-test-link-card",
  );
  const answerPreview = document.getElementById(
    "student-result-answer-preview",
  );
  const test = teacherData.tests.find((entry) => entry.id === result.testId);

  document.getElementById("student-result-course-code").textContent = course
    ? course.code
    : "COURSE";
  document.getElementById("student-result-page-name").textContent =
    getResultStudentName(result);
  document.getElementById("student-result-page-summary").textContent =
    `${getResultTestTitle(result)} · ${level.name} · ${percent}% overall`;
  document.getElementById("student-result-score").textContent = `${percent}%`;
  document.getElementById("student-result-level").textContent = level.name;
  document.getElementById("student-result-earned").textContent =
    `${result.rawScore}/${result.totalPoints}`;
  document.getElementById("student-result-date").textContent = result.date;

  skillList.innerHTML = getResultTopicEntries(result)
    .map((entry) => {
      const topicPercent = getPercent(entry.pointsEarned, entry.totalPoints);
      const topicLevel = getUnderstandingLevel(topicPercent);

      return `
        <div class="student-topic-row">
          <span>${escapeHTML(entry.topic || "General")}</span>
          <strong>${topicPercent}%</strong>
          <span class="badge badge-level ${getLevelClass(topicLevel.name)}">
            ${topicLevel.name}
          </span>
        </div>
      `;
    })
    .join("");

  recommendations.innerHTML = `
    <div class="suggestion-item">
      <strong>Suggested Next Step</strong>
      <p>${getRecommendationForLevel(level.name)}</p>
    </div>

    <div class="suggestion-item">
      <strong>Teacher Follow-Up</strong>
      <p>
        Placeholder note: connect this section to your backend or AI feedback
        generator when real student submissions are available.
      </p>
    </div>
  `;

  if (assessmentReference) {
    assessmentReference.innerHTML = `
      <div class="preview-item">
        <strong>${escapeHTML(getResultTestTitle(result))}</strong>
        <span>${escapeHTML(test ? test.type : "Assessment")} · ${test ? (test.questions || []).length : 0} question(s)</span>
      </div>

      <div class="preview-item">
        <strong>Placeholder integration note</strong>
        <span>Use the button below to reopen the assessment page. Later, connect this section to the real test file and submission record.</span>
      </div>

      <div class="form-actions">
        <button class="btn btn-primary" type="button" onclick="openAssessmentFromStudentResult()">
          Open Assessment
        </button>
      </div>
    `;
  }

  if (answerPreview) {
    answerPreview.innerHTML = getPlaceholderStudentAnswerPreview(result)
      .map((entry) => {
        return `
          <article class="answer-preview-item">
            <div class="answer-preview-top">
              <div>
                <h3>${escapeHTML(entry.label)}</h3>
                <p>${escapeHTML(entry.skill)}</p>
              </div>
              <span class="badge badge-slate">Placeholder Answer</span>
            </div>

            <div class="answer-preview-meta">
              <span>${escapeHTML(entry.topic)}</span>
              <span>${entry.pointsEarned}/${entry.totalPoints} points</span>
            </div>

            <div class="placeholder-answer-box">
              ${escapeHTML(entry.answer)}
            </div>
          </article>
        `;
      })
      .join("");
  }
}

function openAssessmentFromStudentResult() {
  if (selectedTestId) {
    showTestDetail(selectedTestId);
  }
}

function getPlaceholderStudentAnswerPreview(result) {
  return getResultTopicEntries(result).map((entry, index) => {
    return {
      label: `Question ${index + 1}`,
      topic: entry.topic || "General",
      skill: entry.skill || "Submitted skill evidence",
      pointsEarned: entry.pointsEarned,
      totalPoints: entry.totalPoints,
      answer: getPlaceholderStudentResponse(entry, index),
    };
  });
}

function getPlaceholderStudentResponse(entry, index) {
  const templates = [
    `Placeholder student response for ${entry.topic || "this topic"}: the student explains their thinking and shows the final answer here.`,
    `Placeholder free-response work: replace this with the student's actual written answer, trace, or explanation from your backend submission data.`,
    `Placeholder answer record: this can later show selected multiple-choice options, typed responses, or uploaded work for ${entry.skill || "this skill"}.`,
  ];

  return templates[index % templates.length];
}

function getRecommendationForLevel(levelName) {
  const recommendations = {
    Emerging:
      "Review the core concept with guided examples before assigning independent AP-style practice.",
    Developing:
      "Assign targeted practice on weaker topics, then reassess with a short follow-up question set.",
    Proficient:
      "Move into mixed AP-style problems to build consistency across question types.",
    Extending:
      "Offer challenge questions and ask the student to explain reasoning in writing.",
    "Beyond AP":
      "Consider enrichment tasks, peer explanation, or more advanced extension problems.",
  };

  return recommendations[levelName] || recommendations.Developing;
}

function updateResultsSummary(results) {
  const completedCount = results.length;
  const averageScore = getAveragePercent(results);
  const levelCounts = getUnderstandingLevelCounts(results);
  const mostCommonLevel = getMostCommonLevel(levelCounts);

  document.getElementById("results-completed-count").textContent =
    completedCount;
  document.getElementById("results-average-score").textContent =
    completedCount === 0 ? "—" : `${averageScore}%`;
  document.getElementById("results-primary-level").textContent =
    completedCount === 0 ? "—" : mostCommonLevel;
  document.getElementById("results-beyond-count").textContent =
    levelCounts["Beyond AP"] || 0;
}

function renderUnderstandingLevelGrid(results) {
  const levelGrid = document.getElementById("understanding-level-grid");
  const levelCounts = getUnderstandingLevelCounts(results);

  levelGrid.innerHTML = UNDERSTANDING_LEVELS.map((level) => {
    const count = levelCounts[level.name] || 0;

    return `
      <article class="understanding-card ${getLevelClass(level.name)}">
        <div class="understanding-card-head">
          <h3>${level.name}</h3>
          <div class="understanding-count">${count}</div>
        </div>

        <div class="understanding-threshold-row">
          <span class="badge badge-slate understanding-threshold-badge">${level.min}%+</span>
        </div>

        <div class="understanding-card-copy">
          <p>${level.description}</p>
        </div>
      </article>
    `;
  }).join("");
}

function renderTopicUnderstanding(results) {
  const topicList = document.getElementById("topic-understanding-list");
  const topicSummaries = getTopicSummaries(results);

  if (topicSummaries.length === 0) {
    topicList.innerHTML = `
      <div class="empty-state compact">
        <p>No topic-level data yet.</p>
      </div>
    `;
    return;
  }

  topicList.innerHTML = topicSummaries
    .map((topic) => {
      const level = getUnderstandingLevel(topic.percent);

      return `
        <article class="topic-result-card">
          <div class="topic-result-header">
            <div>
              <h3>${escapeHTML(topic.topic)}</h3>
              <p>Class average · ${topic.samples} evidence point(s)</p>
            </div>

            <div class="topic-result-badges">
              <span class="badge badge-slate class-average-badge">Class Average</span>
              <span class="badge badge-level ${getLevelClass(level.name)}">
                ${level.name}
              </span>
            </div>
          </div>

          <div class="level-meter">
            <div class="level-meter-fill" style="width: ${topic.percent}%"></div>
          </div>

          <div class="topic-result-meta">
            <span>${topic.percent}%</span>
            <span>${topic.earned}/${topic.total} points</span>
          </div>
        </article>
      `;
    })
    .join("");
}
function getResultPercent(result) {
  if (
    typeof result.rawScore === "number" &&
    typeof result.totalPoints === "number" &&
    result.totalPoints > 0
  ) {
    return getPercent(result.rawScore, result.totalPoints);
  }

  if (result.score) {
    const parsedScore = Number(String(result.score).replace("%", ""));
    return Number.isFinite(parsedScore) ? parsedScore : 0;
  }

  return 0;
}

function getPercent(pointsEarned, totalPoints) {
  if (!totalPoints || totalPoints <= 0) {
    return 0;
  }

  return Math.round((pointsEarned / totalPoints) * 100);
}

function getAveragePercent(results) {
  if (results.length === 0) {
    return 0;
  }

  const total = results.reduce((sum, result) => {
    return sum + getResultPercent(result);
  }, 0);

  return Math.round(total / results.length);
}

function getUnderstandingLevel(percent) {
  return (
    [...UNDERSTANDING_LEVELS].reverse().find((level) => percent >= level.min) ||
    UNDERSTANDING_LEVELS[0]
  );
}

function getUnderstandingLevelCounts(results) {
  const counts = {};

  UNDERSTANDING_LEVELS.forEach((level) => {
    counts[level.name] = 0;
  });

  results.forEach((result) => {
    const level = getUnderstandingLevel(getResultPercent(result));
    counts[level.name] += 1;
  });

  return counts;
}

function getMostCommonLevel(levelCounts) {
  return Object.entries(levelCounts).sort((a, b) => b[1] - a[1])[0][0];
}

function getTopicSummaries(results) {
  const topicMap = new Map();

  results.forEach((result) => {
    getResultTopicEntries(result).forEach((entry) => {
      const topic = entry.topic || "General";
      const pointsEarned = Number(entry.pointsEarned || 0);
      const totalPoints = Number(entry.totalPoints || 0);

      if (!topicMap.has(topic)) {
        topicMap.set(topic, {
          topic,
          earned: 0,
          total: 0,
          samples: 0,
        });
      }

      const summary = topicMap.get(topic);
      summary.earned += pointsEarned;
      summary.total += totalPoints;
      summary.samples += 1;
    });
  });

  return Array.from(topicMap.values()).map((summary) => {
    return {
      ...summary,
      percent: getPercent(summary.earned, summary.total),
    };
  });
}

function getResultTopicEntries(result) {
  if (
    Array.isArray(result.questionResults) &&
    result.questionResults.length > 0
  ) {
    return result.questionResults;
  }

  if (Array.isArray(result.topicResults) && result.topicResults.length > 0) {
    return result.topicResults;
  }

  return [
    {
      topic: "Overall",
      pointsEarned: result.rawScore || getResultPercent(result),
      totalPoints: result.totalPoints || 100,
    },
  ];
}

function getResultStudentName(result) {
  if (result.student) {
    return result.student;
  }

  const student = teacherData.students.find(
    (student) => student.id === result.studentId,
  );

  return student ? student.name : "Unknown Student";
}

function getResultTestTitle(result) {
  if (result.test) {
    return result.test;
  }

  const test = teacherData.tests.find((test) => test.id === result.testId);

  return test ? test.title : "Unknown Test";
}

function getLevelClass(levelName) {
  return `level-${levelName.toLowerCase().replace(/\s+/g, "-")}`;
}

function escapeHTML(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => {
    const replacements = {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#039;",
    };

    return replacements[character];
  });
}

/* ============================================================
         QUESTION BANK + TEST DRAFT RENDERING
         ============================================================ */

function renderDraftQuestions() {
  const list = document.getElementById("draft-question-list");

  if (!list) {
    return;
  }

  if (draftQuestions.length === 0) {
    list.innerHTML = `
            <div class="empty-state compact">
              <p>No questions added yet. Open the question bank to add MCQs, FRQs, or AI-generated placeholders.</p>
            </div>
          `;
    return;
  }

  list.innerHTML = draftQuestions
    .map((question, index) => {
      return `
            <div class="question-item">
              <div>
                <strong>${index + 1}. ${question.question}</strong>
                <p>${question.type} · ${question.topic || "AI Generated"} · Correct/Rubric: ${question.correct}</p>
              </div>

              <button class="btn btn-sm btn-danger" type="button" onclick="removeDraftQuestion(${index})">
                Remove
              </button>
            </div>
          `;
    })
    .join("");
}

function renderQuestionBank() {
  const mcList = document.getElementById("mc-question-bank-list");
  const frqList = document.getElementById("frq-question-bank-list");

  if (!mcList || !frqList) {
    return;
  }

  const multipleChoiceQuestions = questionBank.filter(
    (question) => question.type === "Multiple Choice",
  );
  const freeResponseQuestions = questionBank.filter(
    (question) => question.type === "Free Response",
  );

  mcList.innerHTML = multipleChoiceQuestions
    .map((question) => renderBankQuestion(question))
    .join("");
  frqList.innerHTML = freeResponseQuestions
    .map((question) => renderBankQuestion(question))
    .join("");
}

function renderBankQuestion(question) {
  return `
          <div class="bank-question">
            <div>
              <div class="bank-meta">
                <span class="badge badge-navy">${question.topic}</span>
                <span class="badge badge-slate">${question.difficulty}</span>
              </div>

              <h3>${question.question}</h3>
              <p>${question.type}</p>
            </div>

            <button class="btn btn-sm btn-primary" type="button" onclick="addBankQuestion('${question.id}')">
              Add to Draft
            </button>
          </div>
        `;
}

function addBankQuestion(questionId) {
  const selectedQuestion = questionBank.find(
    (question) => question.id === questionId,
  );

  if (!selectedQuestion) {
    return;
  }

  draftQuestions.push({
    ...selectedQuestion,
    source: "bank",
  });

  renderDraftQuestions();
  showCourseTab("tests");
}

function generateAIQuestionPlaceholder() {
  const course = getSelectedCourse();
  const topicInput = document.getElementById("ai-topic-input");
  const typeInput = document.getElementById("ai-question-type");
  const difficultyInput = document.getElementById("ai-difficulty");
  const countInput = document.getElementById("ai-question-count");

  const topic =
    topicInput && topicInput.value.trim()
      ? topicInput.value.trim()
      : course
        ? course.unit
        : "Current Unit";
  const selectedType = typeInput ? typeInput.value : "Mixed";
  const difficulty = difficultyInput ? difficultyInput.value : "Adaptive";
  const count = Math.min(5, Math.max(1, Number(countInput?.value) || 1));
  const hasCurriculum = Boolean(course && course.curriculumAttachment);

  const createdQuestions = Array.from({ length: count }, (_, index) => {
    const type =
      selectedType === "Mixed"
        ? index % 2 === 0
          ? "Multiple Choice"
          : "Free Response"
        : selectedType;

    return {
      id: crypto.randomUUID(),
      type: `AI Generated ${type}`,
      topic,
      difficulty,
      question: `${type} placeholder ${index + 1} on ${topic}${hasCurriculum ? " using the uploaded curriculum context" : " using the current course unit"}.`,
      correct:
        type === "Multiple Choice"
          ? "AI-generated answer choice/rationale placeholder"
          : "AI-generated rubric placeholder",
      source: "ai",
    };
  });

  draftQuestions.push(...createdQuestions);
  renderDraftQuestions();
  showCourseTab("tests");
}

function removeDraftQuestion(index) {
  draftQuestions.splice(index, 1);
  renderDraftQuestions();
}

function clearDraftQuestions() {
  draftQuestions = [];
  renderDraftQuestions();
}

/* ============================================================
         COURSE CURRICULUM PDF UPLOAD
         ------------------------------------------------------------
         Frontend-only demo upload. The curriculum PDF is saved with
         the course so AI generation can reference course context later.
         In production, upload the File object to backend storage and
         save only the returned URL/metadata on the course object.
         ============================================================ */

function setupCourseCurriculumUpload() {
  const dropZone = document.getElementById("course-curriculum-drop-zone");
  const fileInput = document.getElementById("course-curriculum-input");

  if (!dropZone || !fileInput || dropZone.dataset.ready === "true") {
    return;
  }

  dropZone.dataset.ready = "true";

  dropZone.addEventListener("click", () => fileInput.click());

  dropZone.addEventListener("keydown", (event) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      fileInput.click();
    }
  });

  fileInput.addEventListener("change", async (event) => {
    const file = event.target.files[0];
    await handleCourseCurriculumFile(file);
  });

  ["dragenter", "dragover"].forEach((eventName) => {
    dropZone.addEventListener(eventName, (event) => {
      event.preventDefault();
      dropZone.classList.add("drag-over");
    });
  });

  ["dragleave", "drop"].forEach((eventName) => {
    dropZone.addEventListener(eventName, (event) => {
      event.preventDefault();
      dropZone.classList.remove("drag-over");
    });
  });

  dropZone.addEventListener("drop", async (event) => {
    const file = event.dataTransfer.files[0];
    await handleCourseCurriculumFile(file);
  });
}

async function handleCourseCurriculumFile(file) {
  if (!file) {
    return;
  }

  const validation = validateCurriculumFile(file);

  if (!validation.success) {
    alert(validation.message);
    clearSelectedCourseFile();
    return;
  }

  selectedCourseFile = {
    id: crypto.randomUUID(),
    name: file.name,
    type: file.type || "application/pdf",
    size: file.size,
    uploadedAt: new Date().toLocaleDateString(),
    dataUrl: await readFileAsDataUrl(file),
  };

  renderSelectedCourseFile();
}

function validateCurriculumFile(file) {
  const maxBytes = APP_CONFIG.maxPdfSizeInMb * 1024 * 1024;
  const isPdf =
    APP_CONFIG.acceptedCurriculumFileTypes.includes(file.type) ||
    file.name.toLowerCase().endsWith(".pdf");

  if (!isPdf) {
    return { success: false, message: "Please upload a PDF file." };
  }

  if (file.size > maxBytes) {
    return {
      success: false,
      message: `That PDF is too large. Max size is ${APP_CONFIG.maxPdfSizeInMb} MB for this demo.`,
    };
  }

  return { success: true };
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

function renderSelectedCourseFile() {
  const preview = document.getElementById("course-curriculum-preview");

  if (!preview) {
    return;
  }

  if (!selectedCourseFile) {
    preview.hidden = true;
    preview.innerHTML = "";
    return;
  }

  preview.hidden = false;
  preview.innerHTML = `
    <div>
      <strong>${escapeHTML(selectedCourseFile.name)}</strong>
      <span>${formatBytes(selectedCourseFile.size)} · Used as AI curriculum context</span>
    </div>
    <button class="btn btn-sm btn-danger" type="button" onclick="clearSelectedCourseFile()">
      Remove PDF
    </button>
  `;
}

function clearSelectedCourseFile() {
  selectedCourseFile = null;

  const fileInput = document.getElementById("course-curriculum-input");
  if (fileInput) {
    fileInput.value = "";
  }

  renderSelectedCourseFile();
}

function formatBytes(bytes) {
  if (!bytes && bytes !== 0) {
    return "Unknown size";
  }

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/* ============================================================
         FORM HANDLERS
         ============================================================ */

document.getElementById("course-form").addEventListener("submit", (event) => {
  event.preventDefault();

  const course = {
    id: crypto.randomUUID(),
    name: document.getElementById("course-name").value.trim(),
    code: document.getElementById("course-code").value.trim(),
    unit: document.getElementById("course-unit").value.trim(),
    pacing: document.getElementById("course-pacing").value,
    description: document.getElementById("course-description").value.trim(),
    curriculumAttachment: selectedCourseFile ? { ...selectedCourseFile } : null,
  };

  teacherData.courses.push(course);
  saveData();

  event.target.reset();
  clearSelectedCourseFile();
  closeCreateCoursePanel();
  renderApp();
});

document
  .getElementById("student-email-form")
  .addEventListener("submit", async (event) => {
    // Stop the page from refreshing when the form submits.
    event.preventDefault();

    // Safety check: the teacher must be inside a course page.
    if (!selectedCourseId) {
      alert("Please open a course first.");
      return;
    }

    // Find the course currently being viewed.
    const course = getSelectedCourse();

    // Build a student object from the form inputs.
    const student = {
      id: crypto.randomUUID(),
      name: document.getElementById("student-email-name").value.trim(),
      email: document.getElementById("student-email-input").value.trim(),
      courseId: selectedCourseId,
      status: "Invite Pending",
    };

    // Try to send the invite email.
    // For now, this calls a backend endpoint placeholder.
    const inviteResult = await sendStudentInviteEmail(student, course);

    if (!inviteResult.success) {
      alert("Invite failed: " + inviteResult.message);
      return;
    }

    // Add the student locally after the invite succeeds.
    teacherData.students.push(student);
    saveData();

    alert(inviteResult.message);

    // Clear the form and redraw the page.
    event.target.reset();
    renderSelectedCourse();
    renderDashboard();
  });

document
  .getElementById("student-request-form")
  .addEventListener("submit", (event) => {
    event.preventDefault();

    if (!selectedCourseId) {
      return;
    }

    const request = {
      id: crypto.randomUUID(),
      name: document.getElementById("request-student-name").value.trim(),
      courseId: selectedCourseId,
    };

    teacherData.joinRequests.push(request);
    saveData();

    event.target.reset();
    renderJoinRequests();
  });

document.getElementById("test-form").addEventListener("submit", (event) => {
  event.preventDefault();

  if (!selectedCourseId) {
    alert("Please open a course first.");
    return;
  }

  if (draftQuestions.length === 0) {
    alert("Please add at least one question before creating the test.");
    return;
  }

  const test = {
    id: crypto.randomUUID(),
    title: document.getElementById("test-title").value.trim(),
    courseId: selectedCourseId,
    type: document.getElementById("test-type").value,
    timeLimit: document.getElementById("test-time").value,
    difficulty: document.getElementById("test-difficulty").value,
    description: document.getElementById("test-description").value.trim(),
    questions: [...draftQuestions],
  };

  teacherData.tests.push(test);
  draftQuestions = [];

  saveData();

  event.target.reset();
  renderApp();
  showCourseTab("tests");
});

/**
 * Temporary demo version.
 * This does NOT send a real email.
 * It pretends the email was sent so the frontend flow keeps working.
 */
async function sendStudentInviteEmail(student, course) {
  console.log("Demo invite email:", {
    to: student.email,
    studentName: student.name,
    courseName: course ? course.name : "Unknown Course",
    courseCode: course ? course.code : "Unknown Code",
  });

  return {
    success: true,
    message:
      "Demo: invite email marked as sent. Backend email system not connected yet.",
  };
}

function acceptJoinRequest(requestId) {
  const request = teacherData.joinRequests.find(
    (request) => request.id === requestId,
  );

  if (!request) {
    return;
  }

  teacherData.students.push({
    id: crypto.randomUUID(),
    name: request.name,
    email: "pending-email@example.com",
    courseId: request.courseId,
    status: "Request Accepted",
  });

  teacherData.joinRequests = teacherData.joinRequests.filter(
    (request) => request.id !== requestId,
  );

  saveData();
  renderSelectedCourse();
  renderDashboard();
}

/* ============================================================
         HELPERS
         ============================================================ */

function getSelectedCourse() {
  return teacherData.courses.find((course) => course.id === selectedCourseId);
}

function getStudentsForCourse(courseId) {
  return teacherData.students.filter(
    (student) => student.courseId === courseId,
  );
}

function getTestsForCourse(courseId) {
  return teacherData.tests.filter((test) => test.courseId === courseId);
}

function getResultsForCourse(courseId) {
  return completedTests.filter((result) => result.courseId === courseId);
}

function clearDemoData() {
  if (!confirm("Clear all demo courses, students, and tests?")) {
    return;
  }

  localStorage.removeItem(STORAGE_KEY);
  teacherData = loadData();
  selectedCourseId = null;
  selectedTestId = null;
  selectedResultId = null;
  draftQuestions = [];
  selectedCourseFile = null;

  renderApp();
  showDashboard();
}

renderApp();

/* ============================================================
   TEACHER GOOGLE SIGN-IN ADDITION
   ------------------------------------------------------------
   This keeps the existing demo sign-in code above, but upgrades the
   public teacher page so it can use the same Google Identity Services
   style of login as the student side.

   What it does:
   1. Renders a real Google button into #teacher-google-signin-container.
   2. Converts the Google ID token into a teacher session.
   3. Tries the backend first: POST /api/auth/google { idToken }.
   4. Falls back to the Google profile claims if the backend is not ready.
   ============================================================ */

function resolveTeacherBackendBase() {
  const host = window.location.hostname;
  const isLocal = host === "localhost" || host === "127.0.0.1";
  return isLocal ? "http://localhost:8080" : "https://cs.andromedax.org";
}

const TEACHER_AUTH_CONFIG = {
  CLIENT_ID:
    "680126421530-3l43rbetvdghr01pq0ecnfpecb49cikj.apps.googleusercontent.com",
  BACKEND_URL: resolveTeacherBackendBase(),
  ALLOWED_EMAIL_DOMAIN: "smus.ca",
};

const TEACHER_SESSION_KEYS = {
  token: "teacherAuthToken",
  teacher: "teacherProfile",
};

// Keep a copy of the original demo sign-in so nothing is deleted.
const signInWithGoogleDemoOriginal = signInWithGoogle;

function isTeacherClientIdConfigured() {
  return (
    TEACHER_AUTH_CONFIG.CLIENT_ID &&
    TEACHER_AUTH_CONFIG.CLIENT_ID !== "PENDING_CLIENT_ID"
  );
}

function decodeTeacherJwtPayload(idToken) {
  const [, payload] = idToken.split(".");

  if (!payload) {
    throw new Error("Invalid Google credential: missing JWT payload.");
  }

  const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
  const json = decodeURIComponent(
    atob(base64)
      .split("")
      .map((char) => "%" + ("00" + char.charCodeAt(0).toString(16)).slice(-2))
      .join(""),
  );

  return JSON.parse(json);
}

function saveTeacherSession({ token, teacher }) {
  sessionStorage.setItem(TEACHER_SESSION_KEYS.token, token);
  sessionStorage.setItem(TEACHER_SESSION_KEYS.teacher, JSON.stringify(teacher));
}

function getTeacherSession() {
  const token = sessionStorage.getItem(TEACHER_SESSION_KEYS.token);
  const rawTeacher = sessionStorage.getItem(TEACHER_SESSION_KEYS.teacher);

  if (!token || !rawTeacher) {
    return null;
  }

  try {
    return {
      token,
      teacher: JSON.parse(rawTeacher),
    };
  } catch (error) {
    console.warn("[Teacher Auth] Saved session could not be read.", error);
    return null;
  }
}

function clearTeacherSession() {
  sessionStorage.removeItem(TEACHER_SESSION_KEYS.token);
  sessionStorage.removeItem(TEACHER_SESSION_KEYS.teacher);
}

function setTeacherAuthMessage(message, kind = "info") {
  const messageBox = document.getElementById("teacher-auth-message");

  if (!messageBox) {
    return;
  }

  messageBox.className = `alert alert-${kind}`;
  messageBox.textContent = message;
}

function applyTeacherSession(teacher, token) {
  teacherData.teacher = {
    name: teacher.name || teacher.email || "Teacher",
    email: teacher.email || "teacher@example.com",
    avatar: teacher.avatar || teacher.picture || null,
  };

  saveTeacherSession({ token, teacher: teacherData.teacher });
  saveData();
  renderApp();
  resetNavigationHistory();
  showDashboard();
}

async function onTeacherGoogleCredential(response) {
  const idToken = response.credential;
  let fallbackTeacher;

  try {
    const claims = decodeTeacherJwtPayload(idToken);
    const email = claims.email || "";

    if (
      TEACHER_AUTH_CONFIG.ALLOWED_EMAIL_DOMAIN &&
      !email.endsWith(`@${TEACHER_AUTH_CONFIG.ALLOWED_EMAIL_DOMAIN}`)
    ) {
      alert(
        `Please sign in with your school account (@${TEACHER_AUTH_CONFIG.ALLOWED_EMAIL_DOMAIN}).`,
      );
      return;
    }

    fallbackTeacher = {
      id: claims.sub,
      name: claims.name || email,
      email,
      avatar: claims.picture || null,
    };
  } catch (error) {
    console.error("[Teacher Auth] Failed to decode Google credential.", error);
    alert("Sign-in failed: could not read the Google response.");
    return;
  }

  try {
    const responseFromBackend = await fetch(
      `${TEACHER_AUTH_CONFIG.BACKEND_URL}/api/auth/google`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken, role: "teacher" }),
      },
    );

    if (responseFromBackend.ok) {
      const data = await responseFromBackend.json();
      const backendTeacher = data.teacher || data.user || fallbackTeacher;
      applyTeacherSession(backendTeacher, data.token || idToken);
      return;
    }

    console.warn(
      "[Teacher Auth] Backend rejected token. Using Google claims fallback.",
    );
  } catch (error) {
    console.warn(
      "[Teacher Auth] Backend unreachable. Using Google claims fallback.",
      error,
    );
  }

  applyTeacherSession(fallbackTeacher, idToken);
}

function initTeacherGoogleAuth() {
  const container = document.getElementById("teacher-google-signin-container");
  const stub = document.getElementById("teacher-google-signin-stub");
  const demoButton = document.getElementById("teacher-demo-signin-button");

  if (!container) {
    return;
  }

  if (!isTeacherClientIdConfigured()) {
    setTeacherAuthMessage(
      "Google sign-in needs a real Google Client ID. Demo mode is still available.",
      "warning",
    );

    if (demoButton) {
      demoButton.hidden = false;
    }
    return;
  }

  if (
    typeof google === "undefined" ||
    !google.accounts ||
    !google.accounts.id
  ) {
    setTeacherAuthMessage(
      "Google sign-in did not load. Try opening the project through localhost/Live Server instead of directly as a file.",
      "warning",
    );

    if (demoButton) {
      demoButton.hidden = false;
    }
    return;
  }

  google.accounts.id.initialize({
    client_id: TEACHER_AUTH_CONFIG.CLIENT_ID,
    callback: onTeacherGoogleCredential,
    auto_select: false,
    hosted_domain: TEACHER_AUTH_CONFIG.ALLOWED_EMAIL_DOMAIN,
  });

  container.innerHTML = "";
  google.accounts.id.renderButton(container, {
    theme: "outline",
    size: "large",
    width: 360,
    text: "signin_with",
    shape: "rectangular",
  });

  container.hidden = false;
  container.style.display = "flex";

  if (stub) {
    stub.hidden = true;
    stub.style.display = "none";
  }

  if (demoButton) {
    demoButton.hidden = false;
  }
}

function restoreTeacherSessionIfPresent() {
  const session = getTeacherSession();

  if (!session) {
    return;
  }

  teacherData.teacher = session.teacher;
  saveData();
  renderApp();
  showDashboard();
}

function signInDemoTeacher() {
  signInWithGoogleDemoOriginal();
}

// Upgrade the existing button name without deleting the original function.
signInWithGoogle = function upgradedTeacherGoogleSignIn() {
  if (typeof google !== "undefined" && google.accounts && google.accounts.id) {
    google.accounts.id.prompt();
    return;
  }

  setTeacherAuthMessage(
    "Google sign-in is not available right now, so demo mode opened instead.",
    "warning",
  );
  signInWithGoogleDemoOriginal();
};

const signOutTeacherOriginal = signOutTeacher;
signOutTeacher = function upgradedTeacherSignOut() {
  clearTeacherSession();

  if (typeof google !== "undefined" && google.accounts && google.accounts.id) {
    google.accounts.id.disableAutoSelect();
  }

  signOutTeacherOriginal();
};

window.onTeacherGoogleCredential = onTeacherGoogleCredential;
window.initTeacherGoogleAuth = initTeacherGoogleAuth;
window.getTeacherSession = getTeacherSession;
window.clearTeacherSession = clearTeacherSession;
window.signInDemoTeacher = signInDemoTeacher;

(function autoInitTeacherGoogleAuth() {
  function pollForGoogle() {
    if (!document.getElementById("teacher-google-signin-container")) {
      return;
    }

    if (
      typeof google !== "undefined" &&
      google.accounts &&
      google.accounts.id
    ) {
      initTeacherGoogleAuth();
      restoreTeacherSessionIfPresent();
      return;
    }

    setTimeout(pollForGoogle, 50);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", pollForGoogle);
  } else {
    pollForGoogle();
  }
})();
