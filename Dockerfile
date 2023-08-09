FROM eclipse-temurin:17

ENV TAPIRZ_HOME="/opt/hochgi/tapirz"

RUN mkdir -p ${TAPIRZ_HOME} \
    && adduser --shell=/bin/bash u \
    && chown -R u:u ${TAPIRZ_HOME}

USER u
WORKDIR ${TAPIRZ_HOME}

COPY --chown=u:u zerver/target/universal/stage ${TAPIRZ_HOME}

EXPOSE 9080 9080

CMD bin/zerver