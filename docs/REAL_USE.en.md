# Before and after in use

[한국어](REAL_USE.md) · [README](../README.md)

These are photographs of the **Galaxy Z Fold3 / Android 15 / One UI 7** used for development, with a damaged center display. The supplied photo files are reproduced unchanged. Select an image to view it at its original size.

## Full-screen Chrome

| Before FoldPatch | After FoldPatch |
| --- | --- |
| [<img src="media/chrome-before-fold3.jpg" width="380" alt="Before: the damaged center obscures text and images in full-screen Chrome." />](media/chrome-before-fold3.jpg) | [<img src="media/chrome-after-fold3.jpg" width="380" alt="After: Chrome uses a narrower page layout with different line wrapping, and the FoldPatch toolbar appears on the left." />](media/chrome-after-fold3.jpg) |

The same webpage reflows to the combined usable width. Text wraps differently because the app receives a narrower layout, rather than being squashed horizontally. The black area remains: it is physical panel damage.

## YouTube on the left, Chrome on the right

| Before FoldPatch | After FoldPatch |
| --- | --- |
| [<img src="media/split-before-fold3.jpg" width="380" alt="Before: YouTube is on the left and Chrome on the right, with damage obscuring their inner edges." />](media/split-before-fold3.jpg) | [<img src="media/split-after-fold3.jpg" width="380" alt="After: YouTube and Chrome keep their left/right arrangement, with a minimized FoldPatch button at the upper left." />](media/split-after-fold3.jpg) |

The existing left/right app arrangement is preserved while the usable width changes. These photographs were taken at different moments, so the video frame and browser toolbar state differ.

## Screen-area preview

| Before adjustment: left 50% · right 50% | After adjustment: left 44% · right 43.5% |
| --- | --- |
| [<img src="media/range-before-fold3.jpg" width="380" alt="With both widths at 50%, the damaged center hides numbers 8 and 9 in the preview." />](media/range-before-fold3.jpg) | [<img src="media/range-after-fold3.jpg" width="380" alt="With the left width at 44% and the right at 43.5%, numbers 1 through 16 are visible across the two sides." />](media/range-after-fold3.jpg) |

Both photos show FoldPatch's screen-area preview. At 50% + 50%, no center gap is reserved. After reducing the widths, the previously obscured numbers 8 and 9 are visible on the two sides. These settings suit the photographed phone; adjust them while looking at your own display.

The webpage and video are examples of existing apps in use. These photos document screen layout; see the [validation record](RELEASE_REVIEW.en.md) for input, recovery and device coverage.
