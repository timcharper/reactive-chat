package main

// import java.nio.CharBuffer
import akka.http.scaladsl.marshalling._
// import akka.http.scaladsl.unmarshalling._
import akka.NotUsed
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
// import play.api.libs.json._

object MarshallingHelpers {
  trait PlainTextWriter[A] {
    def writes(value: A): String
  }

  implicit val stringPlainTextWriter = new PlainTextWriter[String] {
    def writes(value: String): String = value
  }

  def openCharsetStringStreamMarshaller(mediaType: MediaType.WithOpenCharset):
      ToEntityMarshaller[Source[String, Any]] =
    Marshaller.withOpenCharset(mediaType) { (strings, charset) =>
      HttpEntity.Chunked(
        ContentType(mediaType, charset),
        strings.
          filter(_.nonEmpty).
          map { str => HttpEntity.Chunk(str.getBytes(charset.nioCharset))})
    }


  def toPlainTextStream[T](implicit writer: PlainTextWriter[T]): ToEntityMarshaller[Source[T, Any]] =
    openCharsetStringStreamMarshaller(MediaTypes.`text/plain`).
      compose { data =>
        data.
          map( l => writer.writes(l) + "\n")
      }
}
