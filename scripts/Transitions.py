modes = ["SearchMode", "ExtinguishMode", "RefillMode", "MoveToFireMode", "MoveToRefillMode"]

for fromM in modes:
    for toM in modes:
        if fromM == toM:
            continue
        print("(\"{0}\",\"{1}\"),".format(fromM, toM))
    