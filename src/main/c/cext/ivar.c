#include <truffleruby-impl.h>

// Instance variables, rb_iv_*, rb_ivar_*

VALUE rb_obj_instance_variables(VALUE object) {
  return RUBY_CEXT_INVOKE("rb_obj_instance_variables", object);
}

#undef rb_iv_get
VALUE rb_iv_get(VALUE object, const char *name) {
  return RUBY_CEXT_INVOKE("rb_ivar_get", object, ID2SYM(rb_to_id(rb_str_new_cstr(name))));
}

#undef rb_iv_set
VALUE rb_iv_set(VALUE object, const char *name, VALUE value) {
  RUBY_CEXT_INVOKE_NO_WRAP("rb_ivar_set", object, ID2SYM(rb_to_id(rb_str_new_cstr(name))), value);
  return value;
}

VALUE rb_ivar_defined(VALUE object, ID id) {
  return RUBY_CEXT_INVOKE("rb_ivar_defined", object, ID2SYM(id));
}

st_index_t rb_ivar_count(VALUE object) {
  return NUM2ULONG(RUBY_CEXT_INVOKE("rb_ivar_count", object));
}

VALUE rb_ivar_get(VALUE object, ID name) {
  return RUBY_CEXT_INVOKE("rb_ivar_get", object, ID2SYM(name));
}

VALUE rb_ivar_set(VALUE object, ID name, VALUE value) {
  RUBY_CEXT_INVOKE_NO_WRAP("rb_ivar_set", object, ID2SYM(name), value);
  return value;
}

VALUE rb_ivar_lookup(VALUE object, const char *name, VALUE default_value) {
  return rb_tr_wrap(polyglot_invoke(RUBY_CEXT, "rb_ivar_lookup", rb_tr_unwrap(object), name, rb_tr_unwrap(default_value)));
}

// Needed to gem install oj
void rb_ivar_foreach(VALUE obj, int (*func)(ANYARGS), st_data_t arg) {
  rb_tr_error("rb_ivar_foreach not implemented");
}

VALUE rb_attr_get(VALUE object, ID name) {
  return RUBY_CEXT_INVOKE("rb_ivar_lookup", object, ID2SYM(name), Qnil);
}

void rb_copy_generic_ivar(VALUE clone, VALUE obj) {
  RUBY_CEXT_INVOKE_NO_WRAP("rb_copy_generic_ivar", clone, obj);
}

void rb_free_generic_ivar(VALUE obj) {
  RUBY_CEXT_INVOKE_NO_WRAP("rb_free_generic_ivar", obj);
}
