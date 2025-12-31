# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

"맛잘알" (Food Review) - A Korean food review mobile web application with a gamification system where users earn "taste scores" by writing reviews.

## Repository Structure

This is a monorepo with separate directories (each with its own git repository):
- `food-review-frontend/` - Next.js 16 frontend (React 19, TypeScript, Tailwind CSS 4)
- `food-review-backend/` - Spring Boot 3.2 backend (Java 17, JPA, MySQL)
- `food-review-admin/` - Admin 프론트엔드 (Vite + React 19 + TypeScript + TailwindCSS)
- `food-review-admin-backend/` - Admin 백엔드 (Spring Boot 3.2 + Java 17)

## Build & Development Commands

### Frontend (`food-review-frontend/`)
```bash
npm install          # Install dependencies
npm run dev          # Start dev server (localhost:3000)
npm run build        # Build for production (static export)
npm run lint         # Run ESLint
```

### Backend (`food-review-backend/`)
```bash
./gradlew build              # Full build with tests
./gradlew build -x test      # Build without tests (faster)
./gradlew bootRun            # Run the application (localhost:8080)
```

### Admin Frontend (`food-review-admin/`)
```bash
npm run dev          # Start dev server (localhost:3001)
npm run build        # Build for production
```

### Admin Backend (`food-review-admin-backend/`)
```bash
./gradlew bootRun    # Run (localhost:8081)
```

## Architecture

### Frontend
- **Static Export**: `output: 'export'` for S3 hosting
- **API Client**: `lib/api.ts` - JWT auth 처리
- **Auth**: `lib/auth-context.tsx` - JWT tokens in localStorage
- **WebSocket**: `lib/use-chat-socket.ts` - STOMP for real-time chat
- **UI Components**: shadcn/ui in `components/ui/`

### Backend
- **Domain-Driven Structure**: `domain/{feature}/` with controller, service, repository, dto, entity
- **Domains**: auth, user, restaurant, review, chat, image, report
- **Security**: JWT with `JwtTokenProvider`, `@CurrentUser` annotation
- **WebSocket**: STOMP at `/ws` endpoint

## API Patterns

- All responses wrapped in `ApiResponse<T>` with `success`, `data`, `message`
- Paginated responses use `PageResponse<T>`
- REST endpoints under `/api/`
- WebSocket: Subscribe `/topic/chat/{roomUuid}`, Send `/app/chat/{roomUuid}`

## Branch Strategy

- **main**: Production - **ASK USER BEFORE PUSHING**
- **dev**: Development - auto-push allowed

## Git Commit Convention

- 커밋 메시지에 AI 생성 표시 넣지 않음
- 간결하고 명확한 커밋 메시지 (한글 또는 영어)

## Important Conventions

- Korean language for UI text and error messages
- Chat rooms use UUID in URLs (not user IDs)
- Frontend uses optimistic updates for chat
- 하단 네비게이션 높이: 72px (`h-[72px]`)
- 시간 표시: `timeZone: "Asia/Seoul"`

## Environment Variables

### Frontend (`.env.local`)
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_KAKAO_MAP_API_KEY=<kakao_api_key>
NEXT_PUBLIC_KAKAO_CLIENT_ID=<kakao_rest_api_key>
NEXT_PUBLIC_KAKAO_REDIRECT_URI=http://localhost:3000/oauth/kakao/callback
```

### Backend (`application.yml`)
```
DB_PASSWORD, JWT_SECRET, AWS_S3_BUCKET, AWS_REGION, AWS_ACCESS_KEY, AWS_SECRET_KEY
KAKAO_CLIENT_ID, KAKAO_CLIENT_SECRET, KAKAO_REDIRECT_URI
```

## Key Features

### 채팅 기능
- 1:1 채팅 및 단체톡 지원
- 메시지 Long Press 메뉴 (답장, 복사, 삭제, 신고, 차단)
- 답장 형식: `> [메시지ID]발신자: 내용\n\n답장내용`
- `HtmlSanitizer.sanitizeChatMessage()`: 줄바꿈과 `>` 문자 보존

### 사용자 차단/신고
- 차단: 리뷰 피드/채팅방에서 숨김
- 신고: Admin에서 관리 (PENDING → RESOLVED/REJECTED)

### 지역 필터링
- 시/도 → 구/군 → 동 선택 (서울/경기: SVG 지도, 기타: 텍스트 리스트)
- `lib/map-data.ts`: 지도 데이터

### 리뷰 영향력 시스템
- 리뷰 작성 시 "참고한 리뷰" 선택 → 참고된 리뷰어에게 포인트 지급
- 상호 참고 시 포인트 미지급 (어뷰징 방지)

## Mobile App (Capacitor)

```bash
npm run build              # 웹 빌드
npx cap sync android       # Android 동기화
npx cap open android       # Android Studio 열기
```

## Admin System

- Dashboard: `/api/admin/stats`
- Reports: `/api/admin/reports`, `/api/admin/chat-reports`
- 대시보드에 대기 중 신고 수 표시

### 영수증 인증 시스템
- 리뷰 작성 시 영수증 사진 첨부 가능 (선택)
- 영수증 첨부 시 "인증됨" 배지 표시
- 배지 클릭 시 영수증 모달로 확인 가능
- 설정에서 "인증된 리뷰만 보기" 필터 지원
- `lib/feed-settings-context.tsx`: 피드 설정 Context (localStorage 저장)
