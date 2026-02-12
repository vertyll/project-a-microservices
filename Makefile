SERVICES = api-gateway auth-service mail-service role-service user-service
SHARED = shared-infrastructure

.PHONY: build-all test-all clean-all format-all check-style-all run-all stop-all

build-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Building $$dir..."; \
		(cd $$dir && ./gradlew build) || exit 1; \
	done

test-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Testing $$dir..."; \
		(cd $$dir && ./gradlew test) || exit 1; \
	done

clean-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Cleaning $$dir..."; \
		(cd $$dir && ./gradlew clean) || exit 1; \
	done

format-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Formatting $$dir..."; \
		(cd $$dir && ./gradlew ktlintFormat) || exit 1; \
	done

check-style-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Checking style $$dir..."; \
		(cd $$dir && ./gradlew ktlintCheck detekt) || exit 1; \
	done

run-all:
	@echo "Starting all services in background..."
	@for dir in $(SERVICES); do \
		echo "Starting $$dir..."; \
		(cd $$dir && ./gradlew bootRun --args='--spring.profiles.active=dev' > /dev/null 2>&1 &); \
	done
	@echo "All services are starting. Check logs in individual service directories if needed."

stop-all:
	@echo "Stopping all microservices..."
	@jps | grep "Boot" | cut -d " " -f 1 | xargs kill -9 2>/dev/null || echo "No services running."
