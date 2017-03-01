# Gesture
利用annotation集成longpress/drag/sideslip手势

## 效果图

| 侧滑样式 | 长按拖动样式 |
| ------------ | ------------- |
| ![Demo gif](https://github.com/Abeltongtong/Gesture/blob/master/sideslip.gif) | ![Demo gif](https://github.com/Abeltongtong/Gesture/blob/master/longpressdrag.gif)  |

## 使用方式
通过为recyclerview增加annotation即可
```java
    @LongTouch
    @Drag
    @TurnRightSideslip(value = {R.id.image_read3, R.id.image_read4})
    @TurnLeftSideslip(autoClose = true, value = {R.id.image_read, R.id.image_read2})
    RecyclerView list;
```
