sub init()
    m.bg = m.top.findNode("bg")
    m.ring = m.top.findNode("ring")
    m.name = m.top.findNode("name")
end sub

sub paint()
    c = m.top.itemContent
    if c = invalid then return
    if m.watched = invalid or not m.watched.isSameNode(c)
        m.watched = c
        c.observeFieldScoped("ShortDescriptionLine2", "paint")
    end if
    m.name.text = c.title
    sel = (c.ShortDescriptionLine2 = "sel")
    on = m.top.rowListHasFocus and m.top.focusPercent > 0.5
    if sel then m.bg.uri = "pkg:/images/chip_pink.9.png" else m.bg.uri = "pkg:/images/chip_glass.9.png"
    m.ring.visible = on
    if sel or on then m.name.color = "0xFFFFFFFF" else m.name.color = "0xCDB8C9FF"
end sub
