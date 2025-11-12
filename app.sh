#!/bin/bash

# Pastebin Clone Application Manager
# Spring Boot Application Control Script

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_NAME="pastebinclone"
DEBUG_PORT=5005
APP_PORT=8080

# Helper functions
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if PostgreSQL is running
check_postgres() {
    print_info "Checking PostgreSQL connection..."
    if pg_isready -h localhost -p 5432 > /dev/null 2>&1; then
        print_success "PostgreSQL is running"
        return 0
    else
        print_warning "PostgreSQL may not be running on localhost:5432"
        print_warning "Please ensure your database is running before starting the app"
        return 1
    fi
}

# Run the application
run() {
    print_info "Starting $APP_NAME..."
    check_postgres || true
    ./mvnw spring-boot:run
}

# Run in debug mode
debug() {
    print_info "Starting $APP_NAME in DEBUG mode..."
    print_info "Debug port: $DEBUG_PORT"
    check_postgres || true
    ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT"
}

# Build the project
build() {
    print_info "Building $APP_NAME..."
    ./mvnw clean package -DskipTests
    print_success "Build completed! JAR file created in target/"
}

# Build with tests
build_with_tests() {
    print_info "Building $APP_NAME with tests..."
    ./mvnw clean package
    print_success "Build with tests completed!"
}

# Clean the project
clean() {
    print_info "Cleaning project..."
    ./mvnw clean
    print_success "Clean completed!"
}

# Run tests only
test() {
    print_info "Running tests..."
    ./mvnw test
    print_success "Tests completed!"
}

# Install dependencies
install() {
    print_info "Installing dependencies..."
    ./mvnw clean install
    print_success "Dependencies installed!"
}

# Run the packaged JAR
run_jar() {
    print_info "Running packaged JAR..."
    check_postgres || true
    JAR_FILE=$(find target -name "$APP_NAME-*.jar" -not -name "*-sources.jar" | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        print_error "No JAR file found. Please build the project first using: ./app.sh build"
        exit 1
    fi
    print_info "Running: $JAR_FILE"
    java -jar "$JAR_FILE"
}

# Run JAR in debug mode
run_jar_debug() {
    print_info "Running packaged JAR in DEBUG mode..."
    check_postgres || true
    JAR_FILE=$(find target -name "$APP_NAME-*.jar" -not -name "*-sources.jar" | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        print_error "No JAR file found. Please build the project first using: ./app.sh build"
        exit 1
    fi
    print_info "Running: $JAR_FILE"
    print_info "Debug port: $DEBUG_PORT"
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT -jar "$JAR_FILE"
}

# Check application status
status() {
    print_info "Checking application status..."
    if lsof -i :$APP_PORT > /dev/null 2>&1; then
        print_success "Application is running on port $APP_PORT"
        lsof -i :$APP_PORT
    else
        print_warning "No application running on port $APP_PORT"
    fi

    check_postgres || true
}

# Show logs (for packaged app running in background)
logs() {
    print_info "Showing application logs..."
    if [ -f "application.log" ]; then
        tail -f application.log
    else
        print_warning "No log file found at application.log"
        print_info "Run the application with: nohup ./app.sh run-jar > application.log 2>&1 &"
    fi
}

# Display help
show_help() {
    echo ""
    echo -e "${GREEN}Pastebin Clone - Application Manager${NC}"
    echo ""
    echo "Usage: ./app.sh [command]"
    echo ""
    echo "Available commands:"
    echo ""
    echo -e "  ${YELLOW}run${NC}              Run the application in development mode"
    echo -e "  ${YELLOW}debug${NC}            Run the application in debug mode (port $DEBUG_PORT)"
    echo -e "  ${YELLOW}build${NC}            Build the application (skips tests)"
    echo -e "  ${YELLOW}build-test${NC}       Build the application with tests"
    echo -e "  ${YELLOW}clean${NC}            Clean build artifacts"
    echo -e "  ${YELLOW}test${NC}             Run tests only"
    echo -e "  ${YELLOW}install${NC}          Install dependencies"
    echo -e "  ${YELLOW}run-jar${NC}          Run the packaged JAR file"
    echo -e "  ${YELLOW}run-jar-debug${NC}    Run the packaged JAR in debug mode"
    echo -e "  ${YELLOW}status${NC}           Check if application and database are running"
    echo -e "  ${YELLOW}logs${NC}             Show application logs (if running in background)"
    echo -e "  ${YELLOW}help${NC}             Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./app.sh run              # Start development server"
    echo "  ./app.sh debug            # Start with remote debugging"
    echo "  ./app.sh build            # Build without tests"
    echo "  ./app.sh test             # Run tests"
    echo ""
    echo "Database Configuration:"
    echo "  URL: jdbc:postgresql://localhost:5432/pastebin"
    echo "  Username: postgres"
    echo "  Password: postgres"
    echo ""
}

# Main script logic
case "$1" in
    run)
        run
        ;;
    debug)
        debug
        ;;
    build)
        build
        ;;
    build-test)
        build_with_tests
        ;;
    clean)
        clean
        ;;
    test)
        test
        ;;
    install)
        install
        ;;
    run-jar)
        run_jar
        ;;
    run-jar-debug)
        run_jar_debug
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        if [ -z "$1" ]; then
            show_help
        else
            print_error "Unknown command: $1"
            show_help
            exit 1
        fi
        ;;
esac
