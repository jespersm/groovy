def a = {return 1}()
a = {x, y-> return x + y} (1, 2)
a = {x, y-> return x + y} 1, 2
a = {x, y-> return x() + y()} {1} {2}
a = {x, y-> return x + y()}(1) {2}
a = {-> return 1}() + {-> return 2}()
