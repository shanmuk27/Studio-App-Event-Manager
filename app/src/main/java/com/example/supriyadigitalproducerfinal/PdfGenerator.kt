package com.example.supriyadigitalproducerfinal

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createEventBillPdf(context: Context, event: Event, isFinalInvoice: Boolean) {
    val docTitle = if (isFinalInvoice) "I N V O I C E" else "E S T I M A T E"
    val filePrefix = if (isFinalInvoice) "Invoice" else "Estimate"
    val subFolder = if (isFinalInvoice) "Confirmed" else "Analysis"
    val relativePath = Environment.DIRECTORY_DOCUMENTS + "/SupriyaDigital/$subFolder/"

    val totalCost = event.eventItems.sumOf { it.cost * it.quantity } + event.days.sumOf { d -> d.items.sumOf { it.cost * it.quantity } }
    val totalPaid = event.payments.sumOf { it.amount }
    val grandTotal = totalCost - event.discount
    val balanceDue = grandTotal - totalPaid

    val pdfDocument = PdfDocument()

    // --- Premium Brand Color Palette ---
    val brandColor = Color.rgb(26, 35, 126) // Deep Professional Indigo/Blue
    val accentColor = Color.rgb(224, 224, 224) // Clean Grey for lines
    val bgLightColor = Color.rgb(245, 247, 250) // Very light blue/grey for headers
    val textDark = Color.rgb(33, 33, 33)
    val textLight = Color.rgb(97, 97, 97)

    // --- Typography & Paints ---
    val studioNamePaint = Paint().apply { typeface = Typeface.create("serif", Typeface.BOLD); textSize = 26f; color = brandColor; textAlign = Paint.Align.CENTER }
    val subtitlePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); textSize = 11f; color = textLight; textAlign = Paint.Align.CENTER }

    val docTitlePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 22f; color = brandColor; textAlign = Paint.Align.CENTER; letterSpacing = 0.1f }

    val sectionHeaderPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 11f; color = brandColor; letterSpacing = 0.05f }
    val labelPaint = Paint().apply { textSize = 11f; color = textLight }

    val dataTextPaint = TextPaint().apply { textSize = 11f; color = textDark }
    val dataBoldTextPaint = TextPaint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 11f; color = textDark }

    val tableHeaderBgPaint = Paint().apply { color = brandColor; style = Paint.Style.FILL }
    val tableHeaderTextPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 11f; color = Color.WHITE; textAlign = Paint.Align.CENTER } // CENTERED for headers

    // Paints for column alignments
    val colCenterTextPaint = Paint().apply { textSize = 11f; color = textDark; textAlign = Paint.Align.CENTER }
    val moneyPaint = Paint().apply { textSize = 11f; color = textDark; textAlign = Paint.Align.RIGHT }
    val moneyBoldPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 11f; color = textDark; textAlign = Paint.Align.RIGHT }

    val balancePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 16f; color = if (balanceDue > 0) Color.rgb(198, 40, 40) else Color.rgb(46, 125, 50); textAlign = Paint.Align.RIGHT }
    val linePaint = Paint().apply { color = accentColor; strokeWidth = 1f }

    // --- Layout Dimensions ---
    val pageWidth = 595f; val pageHeight = 842f
    val leftMargin = 45f; val rightMargin = pageWidth - 45f
    val topMargin = 50f; val bottomMargin = pageHeight - 60f

    // Columns Configuration (Total width = 505)
    val colDescX = leftMargin + 5f
    val descWidth = 230

    val colQtyCenter = 305f
    val colCostRight = 405f
    val colTotalRight = rightMargin - 5f

    // Header Centers (for "hidings at middle")
    val colDescHeaderCenter = leftMargin + (descWidth / 2f)
    val colCostHeaderCenter = 360f // Midpoint of unit cost column visually
    val colTotalHeaderCenter = 475f // Midpoint of total column visually

    // --- Dynamic Page Manager ---
    class PageManager {
        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), currentPageNum).create()
        var page: PdfDocument.Page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = topMargin

        fun drawWatermark() {
            try {
                val drawable = AppCompatResources.getDrawable(context, R.drawable.logo)
                drawable?.let {
                    // Extract bitmap safely even if it's a VectorDrawable
                    val bm = if (it is BitmapDrawable) {
                        it.bitmap
                    } else {
                        val bitmap = Bitmap.createBitmap(
                            it.intrinsicWidth.takeIf { w -> w > 0 } ?: 400,
                            it.intrinsicHeight.takeIf { h -> h > 0 } ?: 400,
                            Bitmap.Config.ARGB_8888
                        )
                        val bmCanvas = android.graphics.Canvas(bitmap)
                        it.setBounds(0, 0, bmCanvas.width, bmCanvas.height)
                        it.draw(bmCanvas)
                        bitmap
                    }

                    // Strict Aspect Ratio Calculation to fit exactly in middle
                    val maxWatermarkSize = 350f
                    val scale = minOf(maxWatermarkSize / bm.width, maxWatermarkSize / bm.height)
                    val w = (bm.width * scale)
                    val h = (bm.height * scale)

                    // Exact mathematical center of the page
                    val left = (pageWidth - w) / 2f
                    val top = (pageHeight - h) / 2f
                    val destRect = RectF(left, top, left + w, top + h)

                    canvas.drawBitmap(bm, null, destRect, Paint().apply { alpha = 15 }) // 15% opacity
                }
            } catch (e: Exception) { Log.e("PDF", "Watermark error", e) }
        }

        fun drawTableHeader() {
            canvas.drawRect(leftMargin, yPos, rightMargin, yPos + 25f, tableHeaderBgPaint)
            val tY = yPos + 17f
            // Centered Headings
            canvas.drawText("Description", colDescHeaderCenter, tY, tableHeaderTextPaint)
            canvas.drawText("Qty", colQtyCenter, tY, tableHeaderTextPaint)
            canvas.drawText("Unit Cost", colCostHeaderCenter, tY, tableHeaderTextPaint)
            canvas.drawText("Total", colTotalHeaderCenter, tY, tableHeaderTextPaint)
            yPos += 35f
        }

        fun checkPageBreak(requiredSpace: Float, insideTable: Boolean = false) {
            if (yPos + requiredSpace > bottomMargin) {
                pdfDocument.finishPage(page)
                currentPageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), currentPageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = topMargin

                // Redraw top accent band
                canvas.drawRect(0f, 0f, pageWidth, 8f, Paint().apply { color = brandColor })

                drawWatermark() // Always draw watermark exactly in middle of new page
                yPos += 20f

                if (insideTable) {
                    drawTableHeader()
                }
            }
        }
    }

    val pm = PageManager()

    fun drawWrappedText(text: String, x: Float, y: Float, width: Int, paint: TextPaint): Float {
        if (text.isEmpty()) return 0f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.1f).setIncludePad(false).build()
        pm.canvas.save(); pm.canvas.translate(x, y); layout.draw(pm.canvas); pm.canvas.restore()
        return layout.height.toFloat()
    }

    try {
        pm.drawWatermark()

        // Top Accent Band
        pm.canvas.drawRect(0f, 0f, pageWidth, 8f, Paint().apply { color = brandColor })
        pm.yPos += 30f

        // --- Center Aligned Header Section ---
        val centerX = pageWidth / 2f
        pm.canvas.drawText("SUPRIYA DIGITAL STUDIO", centerX, pm.yPos, studioNamePaint)
        pm.yPos += 20f
        pm.canvas.drawText("Photography & Videography  |  Ph: +91 9246789966", centerX, pm.yPos, subtitlePaint)
        pm.yPos += 45f

        // Document Title Center Aligned
        pm.canvas.drawText(docTitle, centerX, pm.yPos, docTitlePaint)
        pm.yPos += 25f
        pm.canvas.drawLine(leftMargin, pm.yPos, rightMargin, pm.yPos, Paint().apply { color = brandColor; strokeWidth = 1.5f })
        pm.yPos += 25f

        // --- Information Grid (Two Columns) ---
        val infoStartY = pm.yPos
        var leftY = infoStartY
        var rightY = infoStartY

        // Left Column: Customer
        pm.canvas.drawText("BILLED TO:", leftMargin, leftY, sectionHeaderPaint); leftY += 18f
        leftY += drawWrappedText(event.customerName.uppercase(), leftMargin, leftY, 220, dataBoldTextPaint) + 6f
        if (event.customerPhone.isNotBlank()) {
            leftY += drawWrappedText("Phone: ${event.customerPhone}", leftMargin, leftY, 220, dataTextPaint)
        }

        // Right Column: Event Details
        val rightColX = 350f
        pm.canvas.drawText("DETAILS:", rightColX, rightY, sectionHeaderPaint); rightY += 18f

        pm.canvas.drawText("Date:", rightColX, rightY, labelPaint)
        pm.canvas.drawText(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()), rightColX + 45f, rightY, dataTextPaint)
        rightY += 18f

        pm.canvas.drawText("Event:", rightColX, rightY, labelPaint)
        drawWrappedText(event.title, rightColX + 45f, rightY - 10f, 150, dataBoldTextPaint)

        pm.yPos = maxOf(leftY, rightY + 15f) + 30f

        // --- Itemized Table ---
        pm.drawTableHeader()

        fun drawRow(name: String, qty: String, cost: Double, total: Double) {
            val layout = StaticLayout.Builder.obtain(name, 0, name.length, dataTextPaint, descWidth).build()
            val rowH = maxOf(layout.height.toFloat(), 18f)

            // Reliably check if we need a new page BEFORE drawing
            pm.checkPageBreak(rowH + 20f, insideTable = true)

            // Draw content
            drawWrappedText(name, colDescX, pm.yPos, descWidth, dataTextPaint)

            val tY = pm.yPos + 10f // Baseline adjustment
            pm.canvas.drawText(qty, colQtyCenter, tY, colCenterTextPaint)
            pm.canvas.drawText(String.format(Locale.getDefault(), "%,.2f", cost), colCostRight, tY, moneyPaint)
            pm.canvas.drawText(String.format(Locale.getDefault(), "%,.2f", total), colTotalRight, tY, moneyBoldPaint)

            pm.yPos += rowH + 10f
            pm.canvas.drawLine(leftMargin, pm.yPos, rightMargin, pm.yPos, linePaint)
            pm.yPos += 10f
        }

        // Event Days Loop
        if (event.days.any { it.items.isNotEmpty() }) {
            event.days.forEach { day ->
                if (day.items.isNotEmpty()) {
                    pm.checkPageBreak(35f, insideTable = true)

                    // Day sub-header band
                    pm.canvas.drawRect(leftMargin, pm.yPos - 14f, rightMargin, pm.yPos + 6f, Paint().apply { color = bgLightColor })
                    pm.canvas.drawText("${day.title} (${day.date})", colDescX, pm.yPos, sectionHeaderPaint)
                    pm.yPos += 18f

                    day.items.forEach { drawRow(it.name, it.quantity.toString(), it.cost, it.cost * it.quantity) }
                }
            }
        }

        // Additional Services Loop
        if (event.eventItems.isNotEmpty()) {
            pm.checkPageBreak(35f, insideTable = true)
            pm.canvas.drawRect(leftMargin, pm.yPos - 14f, rightMargin, pm.yPos + 6f, Paint().apply { color = bgLightColor })
            pm.canvas.drawText("Additional Services", colDescX, pm.yPos, sectionHeaderPaint)
            pm.yPos += 18f
            event.eventItems.forEach { drawRow(it.name, it.quantity.toString(), it.cost, it.cost * it.quantity) }
        }

        // --- Totals Section ---
        pm.checkPageBreak(120f) // Ensure enough room for totals
        pm.yPos += 15f

        val totalsLabelX = colCostRight - 10f

        fun drawTotalLine(label: String, valStr: String, paint: Paint) {
            pm.canvas.drawText(label, totalsLabelX, pm.yPos, Paint(dataTextPaint).apply { textAlign = Paint.Align.RIGHT })
            pm.canvas.drawText(valStr, colTotalRight, pm.yPos, paint)
            pm.yPos += 22f
        }

        pm.canvas.drawLine(totalsLabelX - 60f, pm.yPos - 15f, rightMargin, pm.yPos - 15f, Paint().apply { strokeWidth = 1f; color = brandColor })

        drawTotalLine("Sub Total:", String.format(Locale.getDefault(), "%,.2f", totalCost), moneyPaint)
        if (event.discount > 0) {
            drawTotalLine("Discount:", "- " + String.format(Locale.getDefault(), "%,.2f", event.discount), moneyPaint)
        }

        if (event.payments.isNotEmpty()) {
            event.payments.forEach { p ->
                drawTotalLine("Paid (${p.date}):", "- " + String.format(Locale.getDefault(), "%,.2f", p.amount), moneyPaint)
            }
        }

        pm.canvas.drawLine(totalsLabelX - 60f, pm.yPos - 10f, rightMargin, pm.yPos - 10f, Paint().apply { strokeWidth = 2f; color = brandColor })
        pm.yPos += 15f

        // Highlighted Balance Due
        pm.canvas.drawText(if (isFinalInvoice) "Balance Due" else "Est. Balance", totalsLabelX, pm.yPos, Paint(dataBoldTextPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 13f })
        pm.canvas.drawText(String.format(Locale.getDefault(), "%,.2f", balanceDue), colTotalRight, pm.yPos, balancePaint)

        // --- Footer ---
        pm.checkPageBreak(60f)
        pm.yPos += 40f
        pm.canvas.drawText("Thank you for choosing Supriya Digital Studio!", pageWidth / 2, pm.yPos, Paint(dataBoldTextPaint).apply { textAlign = Paint.Align.CENTER; color = brandColor })
        pm.yPos += 20f
        pm.canvas.drawText(if (isFinalInvoice) "* This invoice confirms the booking for the listed dates." else "* This is an estimate/quotation only. Not a confirmation of booking.", pageWidth / 2, pm.yPos, Paint().apply { textSize = 9f; color = textLight; textAlign = Paint.Align.CENTER })

        // Bottom Accent Band
        pm.canvas.drawRect(0f, pageHeight - 12f, pageWidth, pageHeight, Paint().apply { color = brandColor })

        pdfDocument.finishPage(pm.page)

        // --- File Saving Logic ---
        val safeName = event.customerName.replace(Regex("[^A-Za-z0-9]"), "_")
        val safeTitle = event.title.replace(Regex("[^A-Za-z0-9]"), "_")
        val finalFileName = "${filePrefix}_${safeName}_${safeTitle}.pdf"

        if (isFinalInvoice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val estimateName = "Estimate_${safeName}_${safeTitle}.pdf"
                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                    arrayOf(estimateName, "%SupriyaDigital/Analysis%"), null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        context.contentResolver.delete(ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id), null, null)
                    }
                }
            } catch (e: Exception) { Log.e("PDF", "Could not delete old estimate", e) }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri).use { it?.let { os -> pdfDocument.writeTo(os) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear(); contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, "Saved to Documents/SupriyaDigital/$subFolder", Toast.LENGTH_LONG).show()

            val message = if (isFinalInvoice) "Hello ${event.customerName},\n\nHere is the invoice for ${event.title}.\n\nSupriya Digital Studio"
            else "Hello ${event.customerName},\n\nHere is the bill analysis for ${event.title}.\n\nSupriya Digital Studio"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        }
    } catch (e: Exception) {
        Log.e("PDF", "Error generating PDF", e)
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        pdfDocument.close()
    }
}