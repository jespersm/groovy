['a','b','c'].inject('x') {
    result, item -> item + result + item
}
