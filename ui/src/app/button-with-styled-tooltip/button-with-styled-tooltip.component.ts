import {Component, EventEmitter, Input, Output, ViewEncapsulation} from "@angular/core";
import {NgClass} from "@angular/common";
import {NgbTooltipModule} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'button-with-styled-tooltip',
  standalone: true,
  imports: [NgbTooltipModule, NgClass],
  templateUrl: './button-with-styled-tooltip.component.html',
  styleUrl: './button-with-styled-tooltip.component.css',
  encapsulation: ViewEncapsulation.None
})
export class ButtonWithStyledTooltip {
  @Input() href!: string;
  @Input() btnClass!: string;
  @Input() tooltipText!: string;
  @Input() tooltipType!: string;
  @Output() onClick = new EventEmitter<void>();
}
