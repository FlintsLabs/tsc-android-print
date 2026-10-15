package com.revolic.tscprint.core;

import java.util.stream.IntStream;
public final class PageSelection {
    private PageSelection() { }
    /** Ranges refer to the supplied spooled PDF, after Android's page pruning. */
    public static int[] select(int count, int[][] ranges) {
        if (count < 1 || count > 100) throw new IllegalArgumentException("รองรับ PDF ไม่เกิน 100 หน้า");
        if (ranges == null) return IntStream.range(0,count).toArray();
        boolean[] selected = new boolean[count];
        for (int[] range : ranges) {
            if (range == null || range.length != 2) throw new IllegalArgumentException("Invalid page range");
            int start=range[0], end=range[1];
            if (start==0 && end==Integer.MAX_VALUE) end=count-1;
            if (start<0 || end<start || end>=count) throw new IllegalArgumentException("Page range outside spooled PDF");
            for (int i=start;i<=end;i++) selected[i]=true;
        }
        int[] result=IntStream.range(0,count).filter(i->selected[i]).toArray();
        if (result.length==0) throw new IllegalArgumentException("No pages selected");
        return result;
    }
    public static int totalLabels(int pages, int copies) {
        if (pages<1 || pages>100 || copies<1 || copies>100 || pages*copies>1000) throw new IllegalArgumentException("สูงสุด 100 สำเนา และ 1,000 ฉลากต่องาน");
        return pages*copies;
    }
}
