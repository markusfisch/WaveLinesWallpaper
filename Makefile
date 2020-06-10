PACKAGE = de.markusfisch.android.wavelines

all: debug install start

debug:
	./gradlew assembleDebug

release: lint
	./gradlew assembleRelease

bundle: lint
	./gradlew bundleRelease

lint:
	./gradlew lintDebug

install:
	adb $(TARGET) install -r app/build/outputs/apk/debug/app-debug.apk

start:
	adb $(TARGET) shell 'am start -n \
		$(PACKAGE).debug/$(PACKAGE).activity.SplashActivity'

uninstall:
	adb $(TARGET) uninstall $(PACKAGE).debug

meminfo:
	adb shell dumpsys meminfo $(PACKAGE).debug

images:
	svg/update.sh

avocado:
	avocado $(shell fgrep -rl '<vector' app/src/main/res)

clean:
	./gradlew clean
