// ---------------------------------------------------------------------------
// Shared envelope types (common-lib ApiResponse / Spring Page)
// ---------------------------------------------------------------------------

export interface FieldError {
  field: string;
  message: string;
}

export interface ErrorResponse {
  status: number;
  code: string;
  message: string;
  path?: string;
  fieldErrors?: FieldError[];
  timestamp: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: ErrorResponse;
  timestamp: string;
}

/** Raw Spring Data Page — what paginated endpoints put inside ApiResponse.data. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

// ---------------------------------------------------------------------------
// Enums (string unions matching backend @Enumerated(STRING) names)
// ---------------------------------------------------------------------------

export type RoleName = 'ROLE_USER' | 'ROLE_ADMIN';
export type AuthProvider = 'LOCAL' | 'GOOGLE' | 'GITHUB';

export type ResumeStatus = 'UPLOADED' | 'PROCESSING' | 'ANALYSED' | 'FAILED';

export type InterviewType = 'TECHNICAL' | 'BEHAVIOURAL' | 'MIXED' | 'SYSTEM_DESIGN';
export type QuestionType = 'TECHNICAL' | 'BEHAVIOURAL' | 'SITUATIONAL' | 'CODING';
export type SessionStatus = 'CREATED' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'ABANDONED';

export type MatchStatus = 'PENDING_REVIEW' | 'SAVED' | 'APPLIED' | 'REJECTED';

// ---------------------------------------------------------------------------
// Auth (assumed contract — see README)
// ---------------------------------------------------------------------------

export interface User {
  id: string;
  email: string;
  fullName: string;
  roles: RoleName[];
  provider: AuthProvider;
  emailVerified: boolean;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn?: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface UpdateProfileRequest {
  fullName: string;
}

// ---------------------------------------------------------------------------
// Resume
// ---------------------------------------------------------------------------

export interface Resume {
  id: string;
  userId: string;
  userEmail: string;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  status: ResumeStatus;
  primary: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
  analysedAt: string | null;
}

export interface SkillExtraction {
  skillName: string;
  category: string;
  proficiencyLevel: string;
  yearsUsed: number | null;
  inferredFromContext: boolean;
}

export interface ResumeAnalysis {
  id: string;
  resumeId: string;
  atsScore: number;
  summary: string;
  yearsOfExperience: number | null;
  educationLevel: string | null;
  targetRoles: string[];
  strengths: string[];
  weaknesses: string[];
  suggestions: string[];
  keywords: string[];
  missingKeywords: string[];
  skills: SkillExtraction[];
  createdAt: string;
}

// ---------------------------------------------------------------------------
// Interview
// ---------------------------------------------------------------------------

export interface InterviewSession {
  id: string;
  jobTitle: string;
  targetCompany: string | null;
  type: InterviewType;
  status: SessionStatus;
  totalQuestions: number;
  questionsAnswered: number;
  overallScore: number | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface InterviewQuestion {
  questionId: string;
  questionNumber: number;
  questionText: string;
  type: QuestionType;
  difficulty: string;
  totalQuestions: number;
}

export interface RecommendedResource {
  title: string;
  url: string;
  type: string;
}

export interface InterviewFeedback {
  id: string;
  sessionId: string;
  overallScore: number;
  technicalScore: number;
  behaviouralScore: number;
  communicationScore: number;
  problemSolvingScore: number;
  strongAreas: string[];
  improvementAreas: string[];
  detailedFeedback: string;
  recommendedResources: RecommendedResource[];
  generatedAt: string;
}

export interface InterviewStats {
  totalSessions: number;
  completedSessions: number;
  completionRate: number;
  averageScore: number | null;
}

export interface CreateSessionRequest {
  jobTitle: string;
  jobDescription?: string;
  targetCompany?: string;
  type: InterviewType;
  totalQuestions?: number;
}

// WebSocket incoming (server → client)
export type WsMessageType = 'QUESTION' | 'FEEDBACK' | 'SESSION_COMPLETE' | 'ERROR';

export interface AnswerFeedbackPayload {
  score: number;
  feedback: string;
}

export interface HintPayload {
  hint: string;
}

export interface ErrorPayload {
  message: string;
}

export interface WsMessage {
  type: WsMessageType;
  payload: unknown;
  timestamp: string;
}

// ---------------------------------------------------------------------------
// Job matching
// ---------------------------------------------------------------------------

export interface JobListing {
  id: string;
  title: string;
  company: string;
  location: string | null;
  jobType: string | null;
  descriptionText: string;
  requiredSkills: string[];
  niceToHaveSkills: string[];
  salaryRange: string | null;
  experienceLevel: string | null;
  sourceUrl: string | null;
  postedAt: string | null;
}

export interface JobMatch {
  matchId: string;
  job: JobListing;
  similarityScore: number;
  matchPercentage: number;
  matchedSkills: string[];
  missingSkills: string[];
  status: MatchStatus;
  createdAt: string;
}

export interface PartialSkillMatch {
  skill: string;
  candidateLevel: string;
  requiredLevel: string;
}

export interface SkillGap {
  jobTitle: string;
  matchedSkills: string[];
  missingSkills: string[];
  partialMatches: PartialSkillMatch[];
  gapScore: number;
  readinessLevel: string;
  summary: string;
}

export interface PrioritisedSkill {
  skill: string;
  priority: string;
  estimatedWeeks: number;
  resources: RecommendedResource[];
}

export interface LearningPath {
  jobTitle: string;
  totalEstimatedWeeks: number;
  prioritizedSkills: PrioritisedSkill[];
}

export interface FindMatchesRequest {
  resumeId: string;
  limit?: number;
  targetRole?: string;
}

// ---------------------------------------------------------------------------
// JWT claims (as minted by auth-service, verified by gateway)
// ---------------------------------------------------------------------------

export interface JwtClaims {
  sub: string; // email
  userId: string; // UUID
  roles: string[];
  type: string; // "ACCESS"
  iat: number;
  exp: number;
}
