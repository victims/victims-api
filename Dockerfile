FROM busybox:latest
MAINTAINER Steve Milner

COPY ./victims-api /victims-api

EXPOSE 8080
ENTRYPOINT ["/victims-api"]
CMD ["run"]
