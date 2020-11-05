#include <truffleruby-impl.h>
#include <ruby/encoding.h>

// *printf* functions

VALUE rb_enc_vsprintf(rb_encoding *enc, const char *format, va_list args) {
  char *buffer;
  #ifdef __APPLE__
  if (vasxprintf(&buffer, printf_domain, NULL, format, args) < 0) {
  #else
  if (vasprintf(&buffer, format, args) < 0) {
  #endif
    rb_tr_error("vasprintf error");
  }
  VALUE string = rb_enc_str_new_cstr(buffer, enc);
  free(buffer);
  return string;
}

VALUE rb_enc_sprintf(rb_encoding *enc, const char *format, ...) {
  VALUE result;
  va_list ap;

  va_start(ap, format);
  result = rb_enc_vsprintf(enc, format, ap);
  va_end(ap);

  return result;
}

VALUE rb_sprintf(const char *format, ...) {
  VALUE result;
  va_list ap;

  va_start(ap, format);
  result = rb_vsprintf(format, ap);
  va_end(ap);

  return result;
}

VALUE rb_vsprintf(const char *format, va_list args) {
  return rb_enc_vsprintf(rb_ascii8bit_encoding(), format, args);
}

VALUE rb_f_sprintf(int argc, const VALUE *argv) {
  return RUBY_CEXT_INVOKE("rb_f_sprintf", rb_ary_new4(argc, argv));
}

#undef vsnprintf
int ruby_vsnprintf(char *str, size_t n, char const *fmt, va_list ap) {
  return vsnprintf(str, n, fmt, ap);
}
