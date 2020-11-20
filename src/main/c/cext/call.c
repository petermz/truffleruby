#include <truffleruby-impl.h>

// Calling Ruby methods and blocks from C

int rb_check_arity(int argc, int min, int max) {
  polyglot_invoke(RUBY_CEXT, "rb_check_arity", argc, min, max);
  return argc;
}

NORETURN(void rb_error_arity(int argc, int min, int max)) {
  rb_exc_raise(rb_exc_new3(rb_eArgError, rb_tr_wrap(polyglot_invoke(RUBY_CEXT, "rb_arity_error_string", argc, min, max))));
}

VALUE rb_iterate(VALUE (*function)(), VALUE arg1, VALUE (*block)(), VALUE arg2) {
  return rb_tr_wrap(polyglot_invoke(RUBY_CEXT, "rb_iterate", function, rb_tr_unwrap(arg1), block, rb_tr_unwrap(arg2)));
}

int rb_respond_to(VALUE object, ID name) {
  return RUBY_CEXT_INVOKE("rb_respond_to", object, ID2SYM(name));
}

VALUE rb_funcallv(VALUE object, ID name, int args_count, const VALUE *args) {
  return RUBY_CEXT_INVOKE("rb_funcallv", object, ID2SYM(name), rb_ary_new4(args_count, args));
}

VALUE rb_funcallv_public(VALUE object, ID name, int args_count, const VALUE *args) {
  return RUBY_CEXT_INVOKE("rb_funcallv_public", object, ID2SYM(name), rb_ary_new4(args_count, args));
}

VALUE rb_apply(VALUE object, ID name, VALUE args) {
  return RUBY_CEXT_INVOKE("rb_apply", object, ID2SYM(name), args);
}

VALUE rb_block_call(VALUE object, ID name, int args_count, const VALUE *args, rb_block_call_func_t block_call_func, VALUE data) {
  if (rb_block_given_p()) {
    return rb_funcall_with_block(object, name, args_count, args, rb_block_proc());
  } else if (block_call_func == NULL) {
    return rb_funcallv(object, name, args_count, args);
  } else {
    return rb_tr_wrap(polyglot_invoke(RUBY_CEXT, "rb_block_call", rb_tr_unwrap(object), rb_tr_unwrap(ID2SYM(name)), rb_tr_unwrap(rb_ary_new4(args_count, args)), block_call_func, (void*)data));
  }
}

VALUE rb_each(VALUE array) {
  if (rb_block_given_p()) {
    return rb_funcall_with_block(array, rb_intern("each"), 0, NULL, rb_block_proc());
  } else {
    return RUBY_INVOKE(array, "each");
  }
}

VALUE rb_call_super(int args_count, const VALUE *args) {
  return RUBY_CEXT_INVOKE("rb_call_super", rb_ary_new4(args_count, args));
}

int rb_block_given_p() {
  return !NIL_P(rb_block_proc());
}

VALUE rb_block_proc(void) {
  return RUBY_CEXT_INVOKE("rb_block_proc");
}

VALUE rb_block_lambda(void) {
  return rb_block_proc();
}

VALUE rb_yield(VALUE value) {
  if (rb_block_given_p()) {
    return RUBY_CEXT_INVOKE("rb_yield", value);
  } else {
    return RUBY_CEXT_INVOKE("yield_no_block");
  }
}

VALUE rb_funcall_with_block(VALUE recv, ID mid, int argc, const VALUE *argv, VALUE pass_procval) {
  return RUBY_CEXT_INVOKE("rb_funcall_with_block", recv, ID2SYM(mid), rb_ary_new4(argc, argv), pass_procval);
}

VALUE rb_yield_splat(VALUE values) {
  if (rb_block_given_p()) {
    return RUBY_CEXT_INVOKE("rb_yield_splat", values);
  } else {
    return RUBY_CEXT_INVOKE("yield_no_block");
  }
}

VALUE rb_yield_values(int n, ...) {
  VALUE values = rb_ary_new_capa(n);
  for (int i = 0; i < n; i++) {
    rb_ary_store(values, i, (VALUE) polyglot_get_arg(1+i));
  }
  return rb_yield_splat(values);
}

VALUE rb_yield_values2(int n, const VALUE *argv) {
  VALUE values = rb_ary_new_capa(n);
  for (int i = 0; i < n; i++) {
    rb_ary_store(values, i, (VALUE) argv[i]);
  }
  return rb_yield_splat(values);
}

void *rb_thread_call_with_gvl(gvl_call function, void *data1) {
  return polyglot_invoke(RUBY_CEXT, "rb_thread_call_with_gvl", function, data1);
}

struct gvl_call_data {
  gvl_call function;
  void* data;
};

struct unblock_function_data {
  rb_unblock_function_t* function;
  void* data;
};

static void* call_gvl_call_function(void *data) {
  struct gvl_call_data* s = (struct gvl_call_data*) data;
  return s->function(s->data);
}

static void call_unblock_function(void *data) {
  struct unblock_function_data* s = (struct unblock_function_data*) data;
  s->function(s->data);
}

void *rb_thread_call_without_gvl(gvl_call function, void *data1, rb_unblock_function_t *unblock_function, void *data2) {
  // wrap functions to handle native functions
  struct gvl_call_data call_struct = { function, data1 };
  struct unblock_function_data unblock_struct = { unblock_function, data2 };

  rb_unblock_function_t *wrapped_unblock_function;
  if (unblock_function == RUBY_UBF_IO) {
    wrapped_unblock_function = (rb_unblock_function_t*) rb_tr_unwrap(Qnil);
  } else {
    wrapped_unblock_function = call_unblock_function;
  }

  return polyglot_invoke(RUBY_CEXT, "rb_thread_call_without_gvl",
    call_gvl_call_function, &call_struct,
    wrapped_unblock_function, &unblock_struct);
}

ID rb_frame_this_func(void) {
  return SYM2ID(RUBY_CEXT_INVOKE("rb_frame_this_func"));
}

void rb_need_block(void) {
  if (!rb_block_given_p()) {
    rb_raise(rb_eLocalJumpError, "no block given");
  }
}

void rb_iter_break(void) {
  rb_iter_break_value(Qnil);
}

void rb_iter_break_value(VALUE value) {
  RUBY_CEXT_INVOKE_NO_WRAP("rb_iter_break_value", value);
  rb_tr_error("rb_iter_break_value should not return");
}

const char *rb_sourcefile(void) {
  return RSTRING_PTR(RUBY_CEXT_INVOKE("rb_sourcefile"));
}

int rb_sourceline(void) {
  return polyglot_as_i32(RUBY_CEXT_INVOKE_NO_WRAP("rb_sourceline"));
}

int rb_obj_method_arity(VALUE object, ID id) {
  return polyglot_as_i32(RUBY_CEXT_INVOKE_NO_WRAP("rb_obj_method_arity", object, ID2SYM(id)));
}

int rb_obj_respond_to(VALUE object, ID id, int priv) {
  return polyglot_as_boolean(polyglot_invoke(RUBY_CEXT, "rb_obj_respond_to", rb_tr_unwrap(object), rb_tr_id2sym(id), priv));
}

int rb_method_boundp(VALUE klass, ID id, int ex) {
  return polyglot_as_i32(polyglot_invoke(RUBY_CEXT, "rb_method_boundp", rb_tr_unwrap(klass), rb_tr_id2sym(id), ex));
}

VALUE rb_exec_recursive(VALUE (*func) (VALUE, VALUE, int), VALUE obj, VALUE arg) {
  return rb_tr_wrap(polyglot_invoke(RUBY_CEXT, "rb_exec_recursive", func, rb_tr_unwrap(obj), rb_tr_unwrap(arg)));
}
