AI-POC-PRJ

A proof-of-concept project using Java Spring Boot (backend) and JavaScript (frontend), managed with Gradle.

## Features

- Spring Boot REST API
- JavaScript frontend
- Integration with OpenAI API

## Prerequisites

- Java 17+
- Node.js & npm
- Gradle

## Setup

1. Clone the repository:
   git clone https://github.com/geetav24/AI-POC-PRJ.git cd AI-POC-PRJ

2. 3. Build and run the backend:
   ```bash
   cd backend
   gradle bootRun
   ```
# Usage

- Access the API at `http://localhost:8080`
1. localhost:8080/api/rag-openai/upload
   Upload File
2. localhost:8080/api/rag-openai/chat
   Query the uploaded file