['a','b','c'].inject('x') {
    result, item -> item + result + item
}

println a."${hello}"('world') {
}

println a."${"$hello"}"('world') {
}

a."${"$hello"}" 'world', {
}


